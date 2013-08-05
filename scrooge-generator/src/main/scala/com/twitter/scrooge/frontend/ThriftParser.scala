/*
 * Copyright 2011 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.scrooge.frontend

import scala.collection.mutable
import scala.util.parsing.combinator._
import com.twitter.scrooge._
import java.io.FileNotFoundException
import com.twitter.scrooge.backend._
import scala.Some
import com.twitter.scrooge.frontend.FileParseException

case class FileParseException(filename: String, cause: Throwable)
  extends Exception("Exception parsing: %s".format(filename), cause)

class ThriftParser(
    importer: Importer,
    strict: Boolean,
    defaultOptional: Boolean = false,
    allowOneways: Boolean = false)
  extends RegexParsers {

  import com.twitter.scrooge.ast._

  //                            1    2        3       4         4a    4b 4c       4d
  override val whiteSpace = """(\s+|(//.*\n)|(#.*\n)|(/\*[^\*]([^\*]+|\n|\*(?!/))*\*/))+""".r
  // 1: whitespace, 1 or more
  // 2: leading // followed by anything 0 or more, until \n
  // 3: leading #  followed by anything 0 or more, until \n
  // 4: leading /* then NOT a *, then...
  // 4a:  not a *, 1 or more times
  // 4b:  OR a newline
  // 4c:  OR a * followed by a 0-width lookahead / (not sure why we have this -KO)
  //   (0 or more of 4b/4c/4d)
  // 4d: ending */


  // transformations

  def fixFieldIds(fields: List[Field]): List[Field] = {
    // check negative field ids
    fields.find(_.index < 0).foreach {
      field => throw new NegativeFieldIdException(field.sid.name)
    }

    // check duplicate field ids
    fields.filter(_.index != 0).foldLeft(Set[Int]())((set, field) => {
      if (set.contains(field.index)) {
        throw new DuplicateFieldIdException(field.sid.name)
      } else {
        set + field.index
      }
    })

    var nextId = -1
    fields.map {
      field =>
        if (field.index == 0) {
          val f = field.copy(index = nextId)
          nextId -= 1
          f
        } else {
          field
        }
    }
  }

  // identifier

  /**
   * The places where both SimpleIDs and QualifiedIDs are allowed, in which case
   * we use Identifier:
   *   - right hand side of an assignment
   *   - namespace declaration
   * For all other places, only SimpleIDs are allowed. Specifically
   *   - right hand side of an assignment.
   *
   * Note that Scala parser does not support left recursion well. We cannot do
   * something like this which is more intuitive:

      def qualifiedID = (simpleID <~ "\\.") ~ repsep(simpleID, "\\.".r) ^^ {
        case id ~ ids => QualifiedID((id +: ids) map { _.name })
      }

      def identifier: Parser[Identifier] = qualifiedID | simpleID
   */

  def identifier = "[A-Za-z_][A-Za-z0-9\\._]*".r ^^ {
    x => Identifier(x)
  }

  def simpleID = "[A-Za-z_][A-Za-z0-9_]*".r ^^ {
    x => SimpleID(x)
  }

  // ride hand side (RHS)

  def rhs: Parser[RHS] = {
    numberLiteral | stringLiteral | listOrMapRHS | mapRHS | idRHS |
      failure("constant expected")
  }

  def intConstant = "[-+]?\\d+(?!\\.)".r ^^ {
    x => IntLiteral(x.toLong)
  }

  def numberLiteral = "[-+]?\\d+(\\.\\d+)?([eE][-+]?\\d+)?".r ^^ {
    x =>
      if (x.exists {
        c => "eE." contains c
      }) DoubleLiteral(x.toDouble)
      else IntLiteral(x.toLong)
  }

  def stringLiteral = (("\"" ~> "[^\"]*".r <~ "\"") | ("'" ~> "[^']*".r <~ "'")) ^^ {
    x =>
      StringLiteral(x)
  }

  def listSeparator = "[,;]?".r

  def listOrMapRHS = "[" ~> repsep(rhs, listSeparator) <~ "]" ^^ {
    list =>
      ListRHS(list)
  }

  def mapRHS = "{" ~> repsep(rhs ~ ":" ~ rhs, listSeparator) <~ "}" ^^ {
    list =>
      MapRHS(list.map {
        case k ~ x ~ v => (k, v)
      })
  }

  def idRHS = identifier ^^ {
    id => IdRHS(id)
  }

  // types

  def fieldType: Parser[FieldType] = baseType | containerType | referenceType

  def referenceType = identifier ^^ {
    id => ReferenceType(id)
  }

  def baseType: Parser[BaseType] = (
    "bool" ^^^ TBool |
      "byte" ^^^ TByte |
      "i16" ^^^ TI16 |
      "i32" ^^^ TI32 |
      "i64" ^^^ TI64 |
      "double" ^^^ TDouble |
      "string" ^^^ TString |
      "binary" ^^^ TBinary
    )

  def containerType: Parser[ContainerType] = mapType | setType | listType

  def mapType = ("map" ~> opt(cppType) <~ "<") ~ (fieldType <~ ",") ~ (fieldType <~ ">") ^^ {
    case cpp ~ key ~ value => MapType(key, value, cpp)
  }

  def setType = ("set" ~> opt(cppType)) ~ ("<" ~> fieldType <~ ">") ^^ {
    case cpp ~ t => SetType(t, cpp)
  }

  def listType = ("list" ~ "<") ~> (fieldType <~ ">") ~ opt(cppType) ^^ {
    case t ~ cpp => ListType(t, cpp)
  }

  // FFS. i'm very close to removing this and forcably breaking old thrift files.
  def cppType = "cpp_type" ~> stringLiteral ^^ {
    literal => literal.value
  }

  // fields

  def field = (opt(comments) ~> opt(fieldId) ~ fieldReq) ~
    (fieldType ~ (opt(annotationGroup) ~> simpleID)) ~
    (opt("=" ~> rhs) <~ opt(annotationGroup) <~ opt(listSeparator)) ^^ {
    case (fid ~ req) ~ (ftype ~ sid) ~ value => {
      val transformedVal = ftype match {
        case TBool => value map {
          case IntLiteral(0) => BoolLiteral(false)
          case _ => BoolLiteral(true)
        }
        case _ => value
      }
      // if field is marked optional and a default is defined, ignore the optional part.
      val transformedReq = if (!defaultOptional && transformedVal.isDefined && req.isOptional) Requiredness.Default else req
      Field(fid.getOrElse(0), sid, sid.name, ftype, transformedVal, transformedReq)
    }
  }

  def fieldId = intConstant <~ ":" ^^ {
    x => x.value.toInt
  }

  def fieldReq = opt("required" | "optional") ^^ {
    case Some("required") => Requiredness.Required
    case Some("optional") => Requiredness.Optional
    case None => Requiredness.Default
  }

  // functions

  def function = (opt(comments) ~ (opt("oneway") ~ functionType)) ~ (simpleID <~ "(") ~ (rep(field) <~ ")") ~
    (opt(throws) <~ opt(listSeparator)) ^^ {
    case comment ~ (oneway ~ ftype) ~ id ~ args ~ throws =>
      if (!allowOneways && oneway.isDefined) failOrWarn(new OnewayNotSupportedException(id.fullName))

      Function(
        id,
        id.name,
        if (allowOneways && oneway.isDefined) OnewayVoid else ftype,
        fixFieldIds(args),
        throws.map {
          fixFieldIds(_)
        }.getOrElse(Nil), comment)
  }

  def functionType: Parser[FunctionType] = ("void" ^^^ Void) | fieldType

  def throws = "throws" ~> "(" ~> rep(field) <~ ")"

  // definitions

  def definition = const | typedef | enum | senum | struct | union | exception | service

  def const = opt(comments) ~ ("const" ~> fieldType) ~ simpleID ~ ("=" ~> rhs) ~ opt(listSeparator) ^^ {
    case comment ~ ftype ~ sid ~ const ~ _ => ConstDefinition(sid, ftype, const, comment)
  }

  def typedef = (opt(comments) ~ "typedef") ~> fieldType ~ (opt(annotationGroup) ~> simpleID) ^^ {
    case dtype ~ sid => Typedef(sid, dtype)
  }

  def enum = (opt(comments) ~ (("enum" ~> simpleID) <~ "{")) ~ rep(opt(comments) ~ simpleID ~ opt("=" ~> intConstant) <~
    opt(listSeparator)) <~ "}" ^^ {
    case comment ~ sid ~ items =>
      var failed: Option[Int] = None
      val seen = new mutable.HashSet[Int]
      var nextValue = 0
      val values = new mutable.ListBuffer[EnumField]
      items.foreach {
        case c ~ k ~ v =>
          val value = v.map {
            _.value.toInt
          }.getOrElse(nextValue)
          if (seen contains value) failed = Some(value)
          nextValue = value + 1
          seen += value
          values += EnumField(k, value, c)
      }
      if (failed.isDefined) {
        throw new RepeatingEnumValueException(sid.name, failed.get)
      } else {
        Enum(sid, values.toList, comment)
      }
  }

  def senum = (("senum" ~> simpleID) <~ "{") ~ rep(stringLiteral <~ opt(listSeparator)) <~
    "}" ^^ {
    case sid ~ items => Senum(sid, items.map {
      _.value
    })
  }

  def structLike(keyword: String) =
    (opt(comments) ~ ((keyword ~> simpleID) <~ "{")) ~ rep(field) <~ "}" <~ opt(annotationGroup)

  def struct = structLike("struct") ^^ {
    case comment ~ sid ~ fields => Struct(sid, sid.name, fixFieldIds(fields), comment)
  }

  def union = structLike("union") ^^ {
    case comment ~ sid ~ fields =>
      val fields0 = fields.map {
        case f @ Field(_, _, _, _, _, r) if r == Requiredness.Default => f
        case f @ _ =>
          failOrWarn(UnionFieldRequirednessException(sid.name, f.sid.name, f.requiredness.toString))
          f.copy(requiredness = Requiredness.Default)
      }

      Union(sid, sid.name, fixFieldIds(fields0), comment)
  }

  def exception = (opt(comments) ~ ("exception" ~> simpleID <~ "{")) ~ opt(rep(field)) <~ "}" ^^ {
    case comment ~ sid ~ fields => Exception_(sid, sid.name, fixFieldIds(fields.getOrElse(Nil)), comment)
  }

  def service = (opt(comments) ~ ("service" ~> simpleID)) ~ opt("extends" ~> serviceParentID) ~ ("{" ~> rep(function) <~
    "}") ^^ {
    case comment ~ sid ~ extend ~ functions =>
      Service(sid, extend, functions, comment)
  }

  def serviceParentID = opt(simpleID <~ ".") ~ simpleID ^^ {
    case prefix ~ sid => {
      ServiceParent(sid, prefix)
    }
  }

  // document

  def document: Parser[Document] = rep(header) ~ rep(definition) ^^ {
    case hs ~ ds => Document(hs, ds)
  }

  def header: Parser[Header] = include | cppInclude | namespace

  def include = opt(comments) ~> "include" ~> stringLiteral ^^ {
    s => Include(s.value, parseFile(s.value))
  }

  // bogus dude.
  def cppInclude = "cpp_include" ~> stringLiteral ^^ {
    s => CppInclude(s.value)
  }

  def namespace = opt(comments) ~> "namespace" ~> namespaceScope ~ identifier ^^ {
    case scope ~ id =>
      Namespace(scope, id)
  }

  def namespaceScope = "*" ^^^ "*" | (identifier ^^ {
    id => if (id.fullName == "scala") "java" else id.fullName
  })

  /**
   * Matches scaladoc/javadoc style comments.
   */
  def comments: Parser[String] = {
    rep1(docComment) ^^ {
      case cs =>
        cs.mkString("\n")
    }
  }

  val docComment: Parser[String] = """(?s)/\*\*.+?\*/""".r

  // annotations

  def annotation = identifier ~ ("=" ~> stringLiteral)

  def annotationGroup = "(" ~> repsep(annotation, ",") <~ (opt(",") ~ ")")

  def parse[T](in: String, parser: Parser[T], file: Option[String] = None): T = try {
    parseAll(parser, in) match {
      case Success(result, _) => result
      case x@Failure(msg, z) => throw new ParseException(x.toString)
      case x@Error(msg, _) => throw new ParseException(x.toString)
    }
  } catch {
    case e: Throwable => throw file.map(FileParseException(_, e)).getOrElse(e)
  }

  def parseFile(filename: String): Document = {
    val contents = importer(filename) getOrElse {
      throw new FileNotFoundException(filename)
    }

    val newParser = new ThriftParser(contents.importer, this.strict, defaultOptional, allowOneways)
    newParser.parse(contents.data, newParser.document, Some(filename))
  }

  // helper functions
  def failOrWarn(ex: ParseWarning) {
    if (strict)
      throw ex
    else
      println("Warning: " + ex.getMessage)
  }
}
