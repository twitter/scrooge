package com.twitter.scrooge

object AST {
  case class Document(headers: List[Header], defs: List[Definition])

  sealed abstract class Header
  case class Include(filename: String, document: Document) extends Header
  case class CppInclude(file: String) extends Header
  case class Namespace(scope: String, name: String) extends Header

  sealed abstract class Definition(name: String)

//  case class Constant(name: String, `type`: FieldType, value: ConstValue) extends Definition
// (name)

  sealed abstract class Constant
  case class IntConstant(value: Long) extends Constant
  case class DoubleConstant(value: Double) extends Constant
  case class ListConstant(elems: List[Constant]) extends Constant
  case class MapConstant(elems: Map[Constant, Constant]) extends Constant
  case class StringConstant(value: String) extends Constant
  case class Identifier(name: String) extends Constant

  abstract class FunctionType
  case object Void extends FunctionType
  abstract class FieldType extends FunctionType
  abstract class DefinitionType extends FieldType
  abstract class BaseType extends DefinitionType
  case class ReferenceType(name: String) extends FieldType

  case object TBool extends BaseType
  case object TByte extends BaseType
  case object TI16 extends BaseType
  case object TI32 extends BaseType
  case object TI64 extends BaseType
  case object TDouble extends BaseType
  case object TString extends BaseType
  case object TBinary extends BaseType

  abstract class ContainerType(cppType: Option[String]) extends DefinitionType
  case class MapType(keyType: FieldType, valueType: FieldType, cppType: Option[String]) extends ContainerType(cppType)
  case class SetType(tpe: FieldType, cppType: Option[String]) extends ContainerType(cppType)
  case class ListType(tpe: FieldType, cppType: Option[String]) extends ContainerType(cppType)

  case class Field(var id: Int, name: String, `type`: FieldType, default: Option[Constant],
    optional: Boolean)

  case class Function(name: String, `type`: FunctionType, args: List[Field], oneway: Boolean,
    throws: List[Field])
}
