package com.twitter.scrooge

import scala.collection.mutable
import scala.util.parsing.combinator._
import scala.util.parsing.combinator.lexical._

class ParseException(reason: String, cause: Throwable) extends Exception(reason, cause) {
  def this(reason: String) = this(reason, null)
}

class ScroogeParser extends RegexParsers {
  import AST._

  override val whiteSpace = "(\\s|//.*$|#.*$|/\\*(.*)\\*/)+".r

  // constants
/*
  def constant: Parser[Constant] =
    "const" ~> fieldType ~ identifier ~ ("=" ~> constValue <~ opt(listSeparator)) ^^ {
      case tpe ~ name ~ value => Const(name.name, tpe, value)
    }
*/

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

  def identifier = "[A-Za-z_][A-Za-z0-9\\._]*".r ^^ { x => Identifier(x) }

  // types

  def fieldType: Parser[FieldType] = baseType | containerType | referenceType

  def referenceType = identifier ^^ { x => ReferenceType(x.name) }

  def definitionType = baseType | containerType

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

  def field = (opt(fieldId) ~ opt(fieldReq)) ~ (fieldType ~ identifier) ~ (opt("=" ~> constant) <~
    opt(listSeparator)) ^^ { case (fid ~ req) ~ (ftype ~ id) ~ value =>
    Field(fid.getOrElse(0), id.name, ftype, value, req == Some("optional"))
  }

  def fieldId = intConstant <~ ":" ^^ { x => x.value.toInt }
  def fieldReq = "required" | "optional"

  // functions

  def function = (opt("oneway") ~ functionType) ~ (identifier <~ "(") ~ (rep(field) <~ ")") ~
    (opt(throws) <~ opt(listSeparator)) ^^ { case (oneway ~ ftype) ~ id ~ args ~ throws =>
    Function(id.name, ftype, args, oneway.isDefined, throws.getOrElse(Nil))
  }

  def functionType: Parser[FunctionType] = ("void" ^^^ Void) | fieldType

  def throws = "throws" ~> "(" ~> rep(field) <~ ")"

  // definitions

  def definition = const | typedef | enum | senum | struct | exception | service

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
    var nextValue = 1
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
    case id ~ fields => Struct(id.name, fields)
  }

  def exception = (("exception" ~> identifier) <~ "{") ~ rep(field) <~ "}" ^^ {
    case id ~ fields => Exception_(id.name, fields)
  }

  def service = ("service" ~> identifier) ~ opt("extends" ~> identifier) ~ ("{" ~> rep(function) <~
    "}") ^^ {
    case id ~ extend ~ functions => Service(id.name, extend.map { _.name }, functions)
  }

  // rawr.

  def parse[T](in: String, parser: Parser[T]): T = {
    parseAll(parser, in) match {
      case Success(result, _) => result
      case x @ Failure(msg, z) => throw new ParseException(x.toString)
      case x @ Error(msg, _) => throw new ParseException(x.toString)
    }
  }

}
