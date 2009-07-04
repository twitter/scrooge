package com.twitter.scrooge.parser

object AST {
  def toIDL(e: Tree): String = e match {
    case Document(headers, defs) =>
      headers.map(toIDL).mkString("", "\n", "\n") +
      defs.map(toIDL).mkString("", "\n", "\n")
    case Include(file) =>
      "include \"" + file + "\""
    case CppInclude(file) =>
      "cpp_include \"" + file + "\""
    case Namespace(scope, name) =>
      "namespace " + scope + " " + name
    case Const(name, tpe, value) =>
      "const " + toIDL(tpe) + " " + name + " = " + toIDL(value)
    case Typedef(name, tpe) =>
      "typedef " + toIDL(tpe) + " " + name
    case Enum(name, values) =>
      "\nenum " + name + " {" +
        values.map(toIDL).mkString("\n  ", ",\n  ", "\n") +
      "}\n"
    case Senum(name, values) =>
      "\nsenum " + name + " {" +
        values.map(s => "\"" + s + "\"").mkString("\n  ", ",\n  ", "\n") +
      "}\n"
    case Struct(name, fields) =>
      "\nstruct " + name + " {" +
        fields.map(toIDL).mkString("\n  ", ",\n  ", "\n") +
      "}\n"
    case Exception(name, fields) =>
      "\nexception " + name + " {" +
        fields.map(toIDL).mkString("\n  ", ",\n  ", "\n") +
      "}\n"
    case Service(name, parent, fns) =>
      val ext = parent.map(p => " extends " + p + " ").getOrElse("")
      "\nservice " + name + ext + " {" +
        fns.map(toIDL).mkString("\n  ", ",\n  ", "\n") +
      "}\n"
    case EnumValue(name, value) =>
      val num = if (value > 0) " = " + value else ""
      name + num
    case Field(id, name, tpe, default, required, optional) =>
      val num = if (id > 0) id + ": " else ""
      val dflt = default.map(d => " = " + toIDL(d)).getOrElse("")
      val req = if (required) "required " else ""
      val opt = if (optional) "optional " else ""
      num + req + opt + toIDL(tpe) + " " + name + dflt
    case Function(name, tpe, args, asy, thrws) =>
      val async = if (asy) "async " else ""
      val throws =
        if (thrws.isEmpty) ""
        else thrws.map(toIDL).mkString(" throws (", ",", ")")
      async + toIDL(tpe) + " " + name + args.map(toIDL).mkString("(", ", ", ")") + throws
    case Void => "void"
    case TBool => "bool"
    case TByte => "byte"
    case TI16 => "i16"
    case TI32 => "i32"
    case TI64 => "i64"
    case TDouble => "double"
    case TString => "string"
    case TBinary => "binary"
    case TSList => "slist"
    case MapType(k, v, _) => "map<" + toIDL(k) + ", " + toIDL(v) + ">"
    case SetType(t, _) => "set<" + toIDL(t) + ">"
    case ListType(t, _) => "list<" + toIDL(t) + ">"
    case ReferenceType(t) => t
    case IntConstant(v) => v
    case DoubleConstant(v) => v
    case Identifier(n) => n
    case StringLiteral(s) => "\"" + s + "\""
    case ConstList(ls) => ls.map(toIDL).mkString("[", ", ", "]")
    case ConstMap(m) =>
      (for ((k, v) <- m)
        yield toIDL(k) + ":" + toIDL(v)).mkString("{", ", ", "}")
  }
}

sealed abstract class Tree
case class Document(headers: List[Header], defs: List[Definition]) extends Tree

abstract class Header extends Tree
case class Include(file: String) extends Header
case class CppInclude(file: String) extends Header
case class Namespace(scope: String, name: String) extends Header

abstract class Definition(name: String) extends Tree
case class Const(name: String, tpe: FieldType, value: ConstValue) extends Definition(name)
case class Typedef(name: String, tpe: DefinitionType) extends Definition(name)
case class Enum(name: String, values: List[EnumValue]) extends Definition(name)
case class EnumValue(name: String, var value: Int) extends Tree
case class Senum(name: String, values: List[String]) extends Definition(name)
case class Struct(name: String, fields: List[Field]) extends Definition(name)
case class Exception(name: String, fields: List[Field]) extends Definition(name)
case class Service(name: String, parent: Option[String], functions: List[Function]) extends Definition(name)

case class Field(var id: Int, name: String, tpe: FieldType, default: Option[ConstValue], required: Boolean, optional: Boolean) extends Tree {
  assert(!(required && optional))
}

case class Function(name: String, tpe: FunctionType, args: List[Field], async: Boolean, throws: List[Field]) extends Tree

abstract class FunctionType extends Tree
case object Void extends FunctionType
abstract class FieldType extends FunctionType
abstract class DefinitionType extends FieldType
abstract class BaseType extends DefinitionType
case object TBool extends BaseType
case object TByte extends BaseType
case object TI16 extends BaseType
case object TI32 extends BaseType
case object TI64 extends BaseType
case object TDouble extends BaseType
case object TString extends BaseType
case object TBinary extends BaseType
case object TSList extends BaseType
abstract class ContainerType(cppType: Option[String]) extends DefinitionType
case class MapType(keyType: FieldType, valueType: FieldType, cppType: Option[String]) extends ContainerType(cppType)
case class SetType(tpe: FieldType, cppType: Option[String]) extends ContainerType(cppType)
case class ListType(tpe: FieldType, cppType: Option[String]) extends ContainerType(cppType)
case class ReferenceType(name: String) extends FieldType

object BaseType {
  val map = Map(
    "bool" -> TBool
   ,"byte" -> TByte
   ,"i16" -> TI16
   ,"i32" -> TI32
   ,"i64" -> TI64
   ,"double" -> TDouble
   ,"string" -> TString
   ,"binary" -> TBinary
   ,"slist" -> TSList
  )
}

abstract class ConstValue extends Tree
case class IntConstant(value: String) extends ConstValue
case class DoubleConstant(value: String) extends ConstValue
case class ConstList(elems: List[ConstValue]) extends ConstValue
case class ConstMap(elems: Map[ConstValue, ConstValue]) extends ConstValue
case class StringLiteral(string: String) extends ConstValue
case class Identifier(name: String) extends ConstValue
