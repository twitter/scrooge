package com.twitter.scrooge

import scala.util.parsing.combinator._
import scala.util.parsing.combinator.lexical._

class ParseException(reason: String, cause: Throwable) extends Exception(reason, cause) {
  def this(reason: String) = this(reason, null)
}

class ScroogeParser extends RegexParsers {
  import AST._

  override val whiteSpace = "(\\s|//.*$|#.*$|/\\*(.*)\\*/)".r

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

  def mapType = ("map" ~> opt(cppType)) ~ ("<" ~> fieldType) ~ ("," ~> fieldType) <~ ">" ^^ {
    case cpp ~ key ~ value => MapType(key, value, cpp)
  }

  def setType = ("set" ~> opt(cppType)) ~ ("<" ~> fieldType) <~ ">" ^^ {
    case cpp ~ t => SetType(t, cpp)
  }

  def listType = ("list" ~ "<") ~> (fieldType <~ ">") ~ opt(cppType) ^^ {
    case t ~ cpp => ListType(t, cpp)
  }

  def cppType = "cpp_type" ~> stringConstant ^^ { literal => literal.value }

  // fields

  def field = (opt(fieldId) ~ opt(fieldReq)) ~ (fieldType ~ identifier) ~ (opt("=" ~> constant) ~
    opt(listSeparator)) ^^ { case (fid ~ req) ~ (ftype ~ id) ~ (value ~ _) =>
    Field(fid.getOrElse(0), id.name, ftype, value, req == Some("optional"))
  }

  def fieldId = intConstant <~ ":" ^^ { x => x.value.toInt }
  def fieldReq = "required" | "optional"

  // functions

  def function = (opt("oneway") ~ functionType) ~ (identifier <~ "(") ~ (rep(field) <~ ")") ~
    (opt(throws) ~ opt(listSeparator)) ^^ { case (oneway ~ ftype) ~ id ~ args ~ (throws ~ _) =>
    Function(id.name, ftype, args, oneway.isDefined, throws.getOrElse(Nil))
  }

  def functionType: Parser[FunctionType] = ("void" ^^^ Void) | fieldType

  def throws = "throws" ~> "(" ~> rep(field) <~ ")"

  // rawr.

  def parse[T](in: String, parser: Parser[T]): T = {
    parseAll(parser, in) match {
      case Success(result, _) => result
      case x @ Failure(msg, z) => throw new ParseException(x.toString)
      case x @ Error(msg, _) => throw new ParseException(x.toString)
    }
  }

}

/*

  def constValue:     Parser[ConstValue] =
    intConstant | doubleConstant | literal | identifier | constList | constMap

  def constList:      Parser[ConstList] =
    "[" ~> rep(constValue <~ opt(listSeparator)) <~ "]" ^^ (ConstList.apply _ )

  def constMap:       Parser[ConstMap] =
    "{" ~> rep((constValue <~ ":") ~ constValue <~ opt(listSeparator)) <~ "}" ^^ (x =>
      ConstMap(Map.empty ++ x.map {
        case key ~ value => (key, value)
      }))

  def intConstant:    Parser[IntConstant] =
    accept("int constant", {
      case lexical.NumericLit(n) if !n.contains(".") && !n.contains("e") &&
                                    !n.contains("E") && n.exists(_.isDigit) => IntConstant(n)
    })

  def doubleConstant: Parser[DoubleConstant] =
    accept("double constant", {
      case lexical.NumericLit(n) => DoubleConstant(n)
    })

  def literal:        Parser[StringLiteral] =
    accept("string literal", {
      case lexical.StringLit(s) => StringLiteral(s)
    })

  def identifier:     Parser[Identifier] =
    accept("identifier", {
      case lexical.Identifier(s) if !s.contains("-") => Identifier(s)
    })




import scala.util.parsing.combinator.syntactical._
import scala.util.parsing.input.CharArrayReader.EofCh
**/

/*

class Lexer extends StdLexical with ImplicitConversions {
  override def letter = elem("letter", c => ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')))
  def identChar = letter | digit | elem('.') | elem('_') | elem('-')
  def stringLit1: Parser[StringLit] = '"' ~ rep(chrExcept('"', '\n', EofCh)) ~ '"' ^^ {
    case '"' ~ s ~ '"' => StringLit(s.mkString(""))
  }
  def stringLit2: Parser[StringLit] = '\'' ~ rep(chrExcept('\'', '\n', EofCh)) ~ '\'' ^^ {
    case '\'' ~ s ~ '\'' => StringLit(s.mkString(""))
  }
  def intLit = sign ~ rep1(digit) ^^ { case s ~ d => s + d.mkString("") }
  def numericLit = sign ~ rep(digit) ~ opt(decPart) ~ opt(expPart) ^^ {
    case s ~ i ~ d ~ e => s + i.mkString("") + d.getOrElse("") + e.getOrElse("")
  }
  def sign = opt(elem("sign character", c => c == '-' || c == '+')) ^^ { _.filter(_ == '-').map(_.toString).getOrElse("") }
  def exponent = elem("exponent character", c => c == 'e' || c == 'E')
  def decPart: Parser[String] = '.' ~ rep1(digit) ^^ {
    case '.' ~ d => "." + d.mkString("")
  }
  def expPart: Parser[String] = exponent ~ intLit ^^ {
    case e ~ i => e + i
  }
 */
