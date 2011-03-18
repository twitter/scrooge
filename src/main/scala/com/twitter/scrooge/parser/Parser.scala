package com.twitter.scrooge.parser

import scala.util.parsing.combinator._
import scala.util.parsing.combinator.syntactical._
import scala.util.parsing.combinator.lexical._

class Parser(importer: Importer) extends StdTokenParsers with ImplicitConversions {
  type Tokens = Lexer
  val lexical = new Tokens

  val namespaceScopes = List("*", "cpp", "java", "py", "perl", "rb", "cocoa", "cshapr", "php")
  lexical.reserved ++= List("namespace", "cpp_namespace", "cpp_include", "cpp_type", "java_package",
    "cocoa_prefix", "csharp_namespace", "php_namespace", "py_module", "perl_package",
    "ruby_namespace", "smalltalk.category", "smalltalk_category", "smalltalk.prefix",
    "smalltalk_prefix", "xsd_all", "xsd_optional", "xsd_nillable", "xsd_attrs", "include", "void",
    "senum", "map", "list", "set", "async", "typedef", "struct", "exception", "extends", "throws",
    "service", "enum", "const", "required", "optional", "abstract", "and", "args", "as", "assert",
    "break", "case", "class", "continue", "declare", "def", "default", "del", "delete", "do",
    "elif", "else", "elseif", "except", "exec", "false", "finally", "float", "for", "foreach",
    "function", "global", "goto", "if", "implements", "import", "in", "inline", "instanceof",
    "interface", "is", "lambda", "native", "new", "not", "or", "pass", "public", "print",
    "private", "protected", "raise", "return", "sizeof", "static", "switch", "synchronized", "this",
    "throw", "transient", "true", "try", "unsigned", "var", "virtual", "volatile", "while", "with",
    "union", "yield") ++ BaseType.map.keySet
  lexical.delimiters ++= List("{", "}", "[", "]", "<", ">", "(", ")", ",", ":", ";", "=")

  def document:       Parser[Document] = rep(header) ~ rep(definition) ^^ {
    case hs ~ ds => Document(hs, ds)
  }

  def header:         Parser[Header] =
    include | cppInclude | namespace

  def include:        Parser[Include] =
    "include" ~> literal ^^ (l => Include(l.string, parseFile(l.string)))

  def cppInclude:     Parser[CppInclude] =
    "cpp_include" ~> literal ^^ (l => CppInclude(l.string))

  def namespace:      Parser[Namespace] =
    sm1Namespace | sm2Namespace | triNamespace | phpNamespace | xsdNamespace

  def triNamespace:   Parser[Namespace] =
    "namespace" ~> namespaceScope ~ identifier ^^ {
      case scope ~ name => Namespace(scope, name.name)
    }

  def sm1Namespace:   Parser[Namespace] =
    "namespace" ~> "smalltalk.category" ~> stIdentifier ^^ {
      case name => Namespace("smalltalk.category", name.name)
    }

  def sm2Namespace:   Parser[Namespace] =
    "namespace" ~> "smalltalk.prefix" ~> identifier ^^ {
      case name => Namespace("smalltalk.prefix", name.name)
    }

  def phpNamespace:   Parser[Namespace] =
    "php_namespace" ~> literal ^^ (l => Namespace("php", l.string))

  def xsdNamespace:   Parser[Namespace] =
    "xsd_namespace" ~> literal ^^ (l => Namespace("xsd", l.string))

  def namespaceScope: Parser[String] =
    accept("namespace scope", {
      case lexical.Identifier(s) if namespaceScopes.contains(s) => s
    })

  def definition:     Parser[Definition] =
    const | typedef | enum | senum | struct | exception | service

  def const:          Parser[Const] =
    "const" ~> fieldType ~ identifier ~ ("=" ~> constValue <~ opt(listSeparator)) ^^ {
      case tpe ~ name ~ value => Const(name.name, tpe, value)
    }

  def typedef:        Parser[Typedef] =
    "typedef" ~> definitionType ~ identifier ^^ {
      case tpe ~ name => Typedef(name.name, tpe)
    }

  def enum:           Parser[Enum] =
    "enum" ~> identifier ~ ("{" ~> rep(enumValue) <~ "}")  ^^ {
      case name ~ values => Enum(name.name, values)
    }

  def enumValue:      Parser[EnumValue] =
    identifier ~ opt("=" ~> intConstant) <~ opt(listSeparator) ^^ {
      case name ~ int =>
        int.foreach { i =>
          if (i.value.toInt < 0) {
            throw new ParseException("invalid (negative) enum value " + i.value)
          }
        }
        EnumValue(name.name, int.map(_.value.toInt).getOrElse(-1))
    }

  def senum:          Parser[Senum] =
    "senum" ~> identifier ~ ("{" ~> rep(literal <~ opt(listSeparator)) <~ "}") ^^ {
      case name ~ values => Senum(name.name, values.map(_.string))
    }

  def struct:         Parser[Struct] =
    "struct" ~> identifier ~ (opt("xsd_all") ~> ("{" ~> rep(field) <~ "}")) ^^ {
      case name ~ fields => Struct(name.name, fields)
    }

  def exception:      Parser[Exception_] =
    "exception" ~> identifier ~ ("{" ~> rep(field) <~ "}") ^^ {
      case name ~ fields => Exception_(name.name, fields)
    }

  def service:        Parser[Service] =
    "service" ~> identifier ~ opt("extends" ~> identifier) ~ functionList ^^ {
      case name ~ parent ~ fns => Service(name.name, parent.map(_.name), fns)
    }

  def functionList:   Parser[List[Function]] =
    "{" ~> rep(function) <~ "}"

  def field:          Parser[Field] =
    opt(fieldID) ~ opt(fieldReq) ~ fieldType ~ identifier ~ opt("=" ~> constValue) <~ xsdFieldOptions <~ opt(listSeparator) ^^ {
      case id ~ req ~ tpe ~ name ~ dflt =>
        var idNumber = 0
        var required = false
        var optional = false
        id match {
          case Some(i) => idNumber = i
          case _ => ()
        }
        req match {
          case Some("required") => required = true
          case Some("optional") => optional = true
          case _ => ()
        }
        Field(idNumber, name.name, tpe, dflt, required, optional)
    }

  def fieldID:        Parser[Int] =
    intConstant <~ ":" ^^ (_.value.toInt)

  def fieldReq:       Parser[String] =
    "required" | "optional"

  def xsdFieldOptions =
    opt("xsd_optional") ~ opt("xsd_nillable") ~ opt(xsdAttrs)

  def xsdAttrs:       Parser[List[Field]] =
    "xsd_attrs" ~> ("{" ~> rep(field) <~ "}")

  def funArgs:        Parser[List[Field]] =
    "(" ~> rep(field) <~ ")"

  def function:       Parser[Function] =
    opt("async") ~ functionType ~ identifier ~ funArgs ~ opt(throws) <~ opt(listSeparator) ^^ {
      case async ~ tpe ~ name ~ args ~ throws =>
        Function(name.name, tpe, args, async.isDefined, throws.getOrElse(Nil))
    }

  def functionType:   Parser[FunctionType] =
    (fieldType | "void") ^^ {
      case f: FieldType => f
      case "void" => Void
    }

  def throws:         Parser[List[Field]] =
    "throws" ~> "(" ~> rep(field) <~ ")"

  def fieldType:      Parser[FieldType] =
    (identifier | definitionType) ^^ {
      case tpe: FieldType => tpe
      case Identifier(n) => ReferenceType(n)
      case _ => error("unreachable code")
    }

  def definitionType: Parser[DefinitionType] =
    baseType | containerType

  def baseType:       Parser[BaseType] =
    accept("base type", {
      case lexical.Keyword(n) if BaseType.map.contains(n) => BaseType.map(n)
    })

  def containerType:  Parser[ContainerType] =
    mapType | setType | listType

  def mapType:        Parser[MapType] =
    ("map" ~> opt(cppType) ~ keyType ~ valueType) ^^ {
      case cpp ~ ktpe ~ vtpe => MapType(ktpe, vtpe, cpp)
    }

  def keyType:        Parser[FieldType] =
    "<" ~> fieldType <~ ","

  def valueType:      Parser[FieldType] =
    fieldType <~ ">"

  def setType:        Parser[SetType] =
    "set" ~> opt(cppType) ~ ("<" ~> fieldType <~ ">") ^^ {
      case cpp ~ tpe => SetType(tpe, cpp)
    }

  def listType:       Parser[ListType] =
    "list" ~> ("<" ~> fieldType <~ ">") ~ opt(cppType) ^^ {
      case tpe ~ cpp => ListType(tpe, cpp)
    }

  def cppType:        Parser[String] =
    "cpp_type" ~> literal ^^ (_.string)

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

  def stIdentifier:   Parser[Identifier] =
    accept("smalltalk identifier", {
      case lexical.Identifier(s) => Identifier(s)
    })

  def listSeparator = "," | ";"


  def parse(input: String) =
    phrase(document)(new lexical.Scanner(input)) match {
      case Success(result, _) => result
      case x @ Failure(msg, z) => throw new ParseException(x.toString)
      case x @ Error(msg, _) => throw new ParseException(x.toString)
    }

  def parseFile(filename: String) = EnumValueTransformer.transformDocument(FieldIdTransformer.transformDocument(parse(importer(filename))))
}
