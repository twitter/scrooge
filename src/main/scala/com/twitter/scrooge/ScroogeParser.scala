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

package com.twitter.scrooge

import scala.collection.mutable
import scala.util.parsing.combinator._
import scala.util.parsing.combinator.lexical._
import scala.util.parsing.input.{Positional, StreamReader}
import java.io.{FileInputStream, InputStreamReader, StringReader}

class ParseException(reason: String, cause: Throwable) extends Exception(reason, cause) {
  def this(reason: String) = this(reason, null)
}

class ScroogeParser(importer: Importer) extends RegexParsers {
  import AST._

  override val whiteSpace = """(\s+|(//.*\n)|(#.*\n)|(/\*([^\*]+|\n|\*(?!/))*\*/))+""".r

  // transformations

  def fixFieldIds(fields: List[Field]): List[Field] = {
    if (fields.exists { _.id < 0 })
      throw new ParseException("Negative user-provided field id")

    val explicit = fields.filter { _.id != 0 }
    if (explicit != explicit.distinct)
      throw new ParseException("Duplicate user-provided field id")

    var nextId = -1
    fields.map { field =>
      if (field.id == 0) {
        val f = field.copy(id = nextId)
        nextId -= 1
        f
      } else {
        field
      }
    }
  }

  // constants

  def constant: Parser[Constant] = {
    numberConstant | stringConstant | listConstant | mapConstant | identifier |
      failure("constant expected")
  }

  def intConstant = "[-+]?\\d+(?!\\.)".r ^^ { x => IntConstant(x.toLong) }

  def numberConstant = "[-+]?\\d+(\\.\\d+)?([eE][-+]?\\d+)?".r ^^ { x =>
    if (x.exists { c => "eE." contains c }) DoubleConstant(x.toDouble) else IntConstant(x.toLong)
  }

  def stringConstant = (("\"" ~> "[^\"]*".r <~ "\"") | ("'" ~> "[^']*".r <~ "'")) ^^ { x =>
    StringConstant(x)
  }

  def listSeparator = "[,;]?".r
  def listConstant = "[" ~> repsep(constant, listSeparator) <~ "]" ^^ { list =>
    ListConstant(list)
  }

  def mapConstant = "{" ~> repsep(constant ~ ":" ~ constant, listSeparator) <~ "}" ^^ { list =>
    MapConstant(Map(list.map { case k ~ x ~ v => (k, v) }: _*))
  }

  def identifier = positioned("[A-Za-z_][A-Za-z0-9\\._]*".r ^^ { x => Identifier(x) })

  // types

  def fieldType: Parser[FieldType] = positioned(baseType) | positioned(containerType) | positioned(referenceType)

  def referenceType = identifier ^^ { x => ReferenceType(x.name) }

  def definitionType = positioned(baseType) | positioned(containerType)

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
  def cppType = "cpp_type" ~> stringConstant ^^ { literal => literal.value }

  // fields

  def field = opt(fieldId) ~ fieldReq ~ (fieldType ~ identifier) ~
    (opt("=" ~> constant) <~ opt(listSeparator)) ^^ { case (fid ~ req) ~ (ftype ~ id) ~ value => {
      val transformedVal = ftype match {
        case TBool => value map {
          case IntConstant(0) => BoolConstant(false)
          case _ => BoolConstant(true)
        }
        case _ => value
      }
      // if field is marked optional and a default is defined, ignore the optional part.
      val transformedReq = {
        if (transformedVal.isDefined && req.isOptional) Requiredness.Default else req
      }
      Field(fid.getOrElse(0), id.name, ftype, transformedVal, transformedReq)
    }
  }

  def fieldId = intConstant <~ ":" ^^ { x => x.value.toInt }
  def fieldReq = opt("required" | "optional") ^^ {
    case Some("required") => Requiredness.Required
    case Some("optional") => Requiredness.Optional
    case None => Requiredness.Default
  }

  // functions

  def function = (opt("oneway") ~ functionType) ~ (identifier <~ "(") ~ (rep(field) <~ ")") ~
    (opt(throws) <~ opt(listSeparator)) ^^ { case (oneway ~ ftype) ~ id ~ args ~ throws =>
    Function(id.name, ftype, fixFieldIds(args), oneway.isDefined,
      throws.map { fixFieldIds(_) }.getOrElse(Nil))
  }

  def functionType: Parser[FunctionType] = ("void" ^^^ Void) | fieldType

  def throws = "throws" ~> "(" ~> rep(field) <~ ")"

  // definitions

  def definition = positioned(const) | positioned(typedef) | positioned(enum) | positioned(senum) | positioned(struct) | positioned(exception) | positioned(service)

  def const = "const" ~> fieldType ~ identifier ~ ("=" ~> constant) ~ opt(listSeparator) ^^ {
    case ftype ~ id ~ const ~ _ => Const(id.name, ftype, const)
  }

  def typedef = "typedef" ~> definitionType ~ identifier ^^ {
    case dtype ~ id => Typedef(id.name, dtype)
  }

  def enum = (("enum" ~> identifier) <~ "{") ~ rep(identifier ~ opt("=" ~> intConstant) <~
    opt(listSeparator)) <~ "}" ^^ { case id ~ items =>
    var failed: Option[Int] = None
    val seen = new mutable.HashSet[Int]
    var nextValue = 0
    val values = new mutable.ListBuffer[EnumValue]
    items.foreach { case k ~ v =>
      val value = v.map { _.value.toInt }.getOrElse(nextValue)
      if (seen contains value) failed = Some(value)
      nextValue = value + 1
      seen += value
      values += EnumValue(k.name, value)
    }
    if (failed.isDefined) {
      throw new ParseException("Repeating enum value in " + id.name + ": " + failed.get)
    } else {
      Enum(id.name, values.toList)
    }
  }

  def senum = (("senum" ~> identifier) <~ "{") ~ rep(stringConstant <~ opt(listSeparator)) <~
    "}" ^^ { case id ~ items => Senum(id.name, items.map { _.value })
  }

  def struct = (("struct" ~> identifier) <~ "{") ~ rep(field) <~ "}" ^^ {
    case id ~ fields => Struct(id.name, fixFieldIds(fields))
  }

  def exception = (("exception" ~> identifier) <~ "{") ~ rep(field) <~ "}" ^^ {
    case id ~ fields => Exception_(id.name, fixFieldIds(fields))
  }

  def service = ("service" ~> identifier) ~ opt("extends" ~> identifier) ~ ("{" ~> rep(function) <~
    "}") ^^ {
    case id ~ extend ~ functions =>
      Service(id.name, extend.map { id => ServiceParent(id.name) }, functions)
  }

  // document

  def document: Parser[Document] = rep(header) ~ rep(definition) ^^ {
    case hs ~ ds => Document(hs, ds)
  }

  def header: Parser[Header] = include | cppInclude | namespace

  def include = "include" ~> stringConstant ^^ { s => Include(s.value, parseFile(s.value)) }

  // bogus dude.
  def cppInclude = "cpp_include" ~> stringConstant ^^ { s => CppInclude(s.value) }

  def namespace = "namespace" ~> namespaceScope ~ identifier ^^ { case scope ~ id =>
    Namespace(scope, id.name)
  }
  def namespaceScope = "*" | (identifier ^^ { id => id.name })

  // rawr.
  def parse[T](in: StreamReader, parser: Parser[T]): T = {
    parseAll(parser, in) match {
      case Success(result, _) => result
      case x @ Failure(msg, _) => throw new ParseException(x.toString)
      case x @ Error(msg, _) => throw new ParseException(x.toString)
    }
  }
  def parse[T](in: String, parser: Parser[T]): T = parse(StreamReader(new StringReader(in)), parser)

  def parseFile(filename: String) = parse(StreamReader(new InputStreamReader(new FileInputStream((filename)))), document)
}
