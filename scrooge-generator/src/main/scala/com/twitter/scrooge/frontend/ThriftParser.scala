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

import com.twitter.scrooge.ast._
import java.io.FileNotFoundException
import scala.collection.concurrent.{Map, TrieMap}
import scala.collection.mutable
import scala.util.parsing.combinator._

case class FileParseException(filename: String, cause: Throwable)
  extends Exception("Exception parsing: %s".format(filename), cause)

class ThriftParser(
  importer: Importer,
  strict: Boolean,
  defaultOptional: Boolean = false,
  skipIncludes: Boolean = false,
  documentCache: Map[String, Document] = new TrieMap[String, Document]
) extends RegexParsers {


  //                            1    2        3                   4         4a    4b 4c       4d
  override val whiteSpace = """(\s+|(//.*\n)|(#([^@\n][^\n]*)?\n)|(/\*[^\*]([^\*]+|\n|\*(?!/))*\*/))+""".r
  // 1: whitespace, 1 or more
  // 2: leading // followed by anything 0 or more, until \n
  // 3: leading #  then NOT a @ followed by anything 0 or more, until \n
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

  val identifierRegex = "[A-Za-z_][A-Za-z0-9\\._]*".r
  lazy val identifier = identifierRegex ^^ {
    x => Identifier(x)
  }

  private[this] val thriftKeywords = Set[String](
    "async",
    "const",
    "enum",
    "exception",
    "extends",
    "include",
    "namespace",
    "optional",
    "required",
    "service",
    "struct",
    "throws",
    "typedef",
    "union",
    "void",
    // Built-in types are also keywords.
    "binary",
    "bool",
    "byte",
    "double",
    "i16",
    "i32",
    "i64",
    "list",
    "map",
    "set",
    "string"
  )

  lazy val simpleIDRegex = "[A-Za-z_][A-Za-z0-9_]*".r
  lazy val simpleID = simpleIDRegex ^^ { x =>
    if (thriftKeywords.contains(x))
      failOrWarn(new KeywordException(x))

    SimpleID(x)
  }

  // ride hand side (RHS)

  lazy val rhs: Parser[RHS] = {
    numberLiteral | stringLiteral | listOrMapRHS | mapRHS | idRHS |
      failure("constant expected")
  }

  lazy val intConstant = "[-+]?\\d+(?!\\.)".r ^^ {
    x => IntLiteral(x.toLong)
  }

  lazy val numberLiteral = "[-+]?\\d+(\\.\\d+)?([eE][-+]?\\d+)?".r ^^ {
    x =>
      if (x.exists { c => "eE." contains c }) DoubleLiteral(x.toDouble)
      else IntLiteral(x.toLong)
  }

  // use a single regex to match string quote-to-quote, so that whitespace parser doesn't
  // get executed inside the quotes
  lazy val doubleQuotedString = """(")(\\.|[^\\"])*(")""".r
  lazy val singleQuotedString = """'(\\.|[^\\'])*'""".r

  lazy val stringLiteral = (doubleQuotedString | singleQuotedString) ^^ {
    // strip off quotes
    x => StringLiteral(x.substring(1, x.length - 1))
  }

  lazy val listSeparator = "[,;]?".r

  lazy val listOrMapRHS = "[" ~> repsep(rhs, listSeparator) <~ opt(listSeparator) <~ "]" ^^ {
    list => ListRHS(list)
  }

  lazy val keyval = rhs ~ (":" ~> rhs) ^^ {
    case k ~ v => (k, v)
  }

  lazy val mapRHS = "{" ~> repsep(keyval, listSeparator) <~ opt(listSeparator) <~ "}" ^^ {
    list => MapRHS(list)
  }

  lazy val idRHS = identifier ^^ {
    id => IdRHS(id)
  }

  // types

  lazy val fieldType: Parser[FieldType] = baseType | containerType | referenceType

  lazy val referenceType = identifier ^^ {
    id => ReferenceType(id)
  }

  lazy val baseType: Parser[BaseType] = (
    "bool" ^^^ TBool |
      "byte" ^^^ TByte |
      "i16" ^^^ TI16 |
      "i32" ^^^ TI32 |
      "i64" ^^^ TI64 |
      "double" ^^^ TDouble |
      "string" ^^^ TString |
      "binary" ^^^ TBinary
    )

  lazy val containerType: Parser[ContainerType] = mapType | setType | listType

  lazy val mapType = ("map" ~> opt(cppType) <~ "<") ~ (fieldType <~ ",") ~ (fieldType <~ ">") ^^ {
    case cpp ~ key ~ value => MapType(key, value, cpp)
  }

  lazy val setType = ("set" ~> opt(cppType)) ~ ("<" ~> fieldType <~ ">") ^^ {
    case cpp ~ t => SetType(t, cpp)
  }

  lazy val listType = ("list" ~ "<") ~> (fieldType <~ ">") ~ opt(cppType) ^^ {
    case t ~ cpp => ListType(t, cpp)
  }

  // FFS. i'm very close to removing this and forcably breaking old thrift files.
  lazy val cppType = "cpp_type" ~> stringLiteral ^^ {
    literal => literal.value
  }

  // fields

  lazy val field = (opt(comments) ~> opt(fieldId) ~ fieldReq) ~
    (fieldType ~ defaultedAnnotations ~ simpleID) ~
    opt("=" ~> rhs) ~ defaultedAnnotations <~ opt(listSeparator) ^^ {
      case (fid ~ req) ~ (ftype ~ typeAnnotations ~ sid) ~ value ~ fieldAnnotations => {
        val transformedVal = ftype match {
          case TBool => value map {
            case IntLiteral(0) => BoolLiteral(false)
            case _ => BoolLiteral(true)
          }
          case _ => value
        }

        // if field is marked optional and a default is defined, ignore the optional part.
        val transformedReq = if (!defaultOptional && transformedVal.isDefined && req.isOptional) Requiredness.Default else req

        Field(
          fid.getOrElse(0),
          sid,
          sid.name,
          ftype,
          transformedVal,
          transformedReq,
          typeAnnotations,
          fieldAnnotations
        )
    }
  }

  lazy val fieldId = intConstant <~ ":" ^^ {
    x => x.value.toInt
  }

  lazy val fieldReq = opt("required" | "optional") ^^ {
    case Some("required") => Requiredness.Required
    case Some("optional") => Requiredness.Optional
    case None => Requiredness.Default
  }

  // functions

  lazy val function = (opt(comments) ~ (opt("oneway") ~ functionType)) ~ (simpleID <~ "(") ~ (rep(field) <~ ")") ~
    (opt(throws) <~ opt(listSeparator)) ^^ {
    case comment ~ (oneway ~ ftype) ~ id ~ args ~ throws =>
      Function(
        id,
        id.name,
        if (oneway.isDefined) OnewayVoid else ftype,
        fixFieldIds(args),
        throws.map {
          fixFieldIds(_)
        }.getOrElse(Nil), comment)
  }

  lazy val functionType: Parser[FunctionType] = ("void" ^^^ Void) | fieldType

  lazy val throws = "throws" ~> "(" ~> rep(field) <~ ")"

  // definitions

  lazy val definition = const | typedef | enum | senum | struct | union | exception | service

  lazy val const = opt(comments) ~ ("const" ~> fieldType) ~ simpleID ~ ("=" ~> rhs) ~ opt(listSeparator) ^^ {
    case comment ~ ftype ~ sid ~ const ~ _ => ConstDefinition(sid, ftype, const, comment)
  }

  lazy val typedef = (opt(comments) ~ "typedef") ~> fieldType ~ defaultedAnnotations ~ simpleID ^^ {
    case dtype ~ annotations ~ sid => Typedef(sid, dtype, annotations)
  }

  lazy val enum = (opt(comments) ~ (("enum" ~> simpleID) <~ "{")) ~ rep(opt(comments) ~ simpleID ~ opt("=" ~> intConstant) <~
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

  lazy val senum = (("senum" ~> simpleID) <~ "{") ~ rep(stringLiteral <~ opt(listSeparator)) <~
    "}" ^^ {
    case sid ~ items => Senum(sid, items.map {
      _.value
    })
  }

  def structLike(keyword: String) =
    (opt(comments) ~ ((keyword ~> simpleID) <~ "{")) ~ rep(field) ~ ("}" ~> defaultedAnnotations)

  lazy val struct = structLike("struct") ^^ {
    case comment ~ sid ~ fields ~ annotations =>
      Struct(sid, sid.name, fixFieldIds(fields), comment, annotations)
  }

  private[this] val disallowedUnionFieldNames = Set("unknown_union_field", "unknownunionfield") map { _.toLowerCase }

  lazy val union = structLike("union") ^^ {
    case comment ~ sid ~ fields ~ annotations =>
      val fields0 = fields.map {
        case f @ Field(_, _, _, _, _, r, _, _) if r == Requiredness.Default =>
          if (disallowedUnionFieldNames.contains(f.sid.name.toLowerCase)) {
            throw new UnionFieldInvalidNameException(sid.name, f.sid.name)
          } else f
        case f @ _ =>
          failOrWarn(UnionFieldRequirednessException(sid.name, f.sid.name, f.requiredness.toString))
          f.copy(requiredness = Requiredness.Default)
      }

      Union(sid, sid.name, fixFieldIds(fields0), comment, annotations)
  }

  lazy val exception = (opt(comments) ~ ("exception" ~> simpleID <~ "{")) ~ opt(rep(field)) <~ "}" ^^ {
    case comment ~ sid ~ fields => Exception_(sid, sid.name, fixFieldIds(fields.getOrElse(Nil)), comment)
  }

  lazy val service = (opt(comments) ~ ("service" ~> simpleID)) ~ opt("extends" ~> serviceParentID) ~ ("{" ~> rep(function) <~
    "}") ^^ {
    case comment ~ sid ~ extend ~ functions =>
      Service(sid, extend, functions, comment)
  }

  // This is a simpleID without the keyword check. Filenames that are thrift keywords are allowed.
  lazy val serviceParentID = opt(simpleIDRegex <~ ".") ~ simpleID ^^ {
    case prefix ~ sid => {
      ServiceParent(sid, prefix.map(SimpleID(_)))
    }
  }

  // document

  lazy val document: Parser[Document] = rep(header) ~ rep(definition) <~ opt(comments) ^^ {
    case hs ~ ds => Document(hs, ds)
  }

  lazy val header: Parser[Header] = include | cppInclude | namespace

  lazy val include = opt(comments) ~> "include" ~> stringLiteral ^^ { s =>
    val doc =
      if (skipIncludes) {
        Document(Seq(), Seq())
      } else {
        parseFile(s.value)
      }
    Include(s.value, doc)
  }

  // bogus dude.
  lazy val cppInclude = "cpp_include" ~> stringLiteral ^^ {
    s => CppInclude(s.value)
  }

  lazy val namespace = opt(comments) ~> opt("#@") ~> "namespace" ~> namespaceScope ~ identifier ^^ {
    case scope ~ id =>
      Namespace(scope, id)
  }

  lazy val namespaceScope = "*" ^^^ "*" | (identifier ^^ { _.fullName })

  /**
   * Matches scaladoc/javadoc style comments.
   */
  lazy val comments: Parser[String] = {
    rep1(docComment) ^^ {
      case cs =>
        cs.mkString("\n")
    }
  }

  val docComment: Parser[String] = """(?s)/\*\*.+?\*/""".r

  // annotations

  lazy val annotation = identifier ~ ("=" ~> stringLiteral) ^^ {
    case id ~ StringLiteral(value) => id.fullName -> value
  }

  lazy val annotationGroup = "(" ~> repsep(annotation, ",") <~ (opt(",") ~ ")") ^^ { _.toMap }

  lazy val defaultedAnnotations = opt(annotationGroup) ^^ { _ getOrElse Map.empty }

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
    importer.getResolvedPath(filename) match {
      // Cache the result if the importer supports caching.
      case Some(key) => documentCache.getOrElseUpdate(key, parseFileUncached(filename))
      // Else, just resolve the document.
      case None => parseFileUncached(filename)
    }
  }

  private[this] def parseFileUncached(filename: String): Document = {
    val contents = importer(filename) getOrElse {
      throw new FileNotFoundException(filename)
    }
    // one thrift file can be included in another and referenced like this:
    // list<includedthriftfilenamehere.Request> requests
    //
    // thus we need to ensure includedthriftfilenamehere is valid, otherwise the first person
    // to include the thrift file, with for example a dash in the name, will run into problems
    contents.thriftFilename foreach { f =>
      identifierRegex.findFirstIn(f) match {
        case Some(`f`) => ()
        case _ => failOrWarn(new InvalidThriftFilenameException(f, identifierRegex.toString()))
      }
    }

    val newParser = new ThriftParser(contents.importer,
      this.strict,
      this.defaultOptional,
      this.skipIncludes,
      this.documentCache)
    newParser.parse(contents.data, newParser.document, contents.thriftFilename)
  }

  // helper functions
  def failOrWarn(ex: ParseWarning) {
    if (strict)
      throw ex
    else
      println("Warning: " + ex.getMessage)
  }
}
