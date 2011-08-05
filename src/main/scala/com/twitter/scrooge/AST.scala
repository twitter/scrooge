package com.twitter.scrooge

object AST {
  sealed abstract class Requiredness {
    def isOptional = this eq Requiredness.Optional
    def isRequired = this eq Requiredness.Required
    def isDefault = this eq Requiredness.Default
  }
  object Requiredness {
    case object Optional extends Requiredness
    case object Required extends Requiredness
    case object Default extends Requiredness
  }

  sealed abstract class Constant
  case class BoolConstant(value: Boolean) extends Constant
  case class IntConstant(value: Long) extends Constant
  case class DoubleConstant(value: Double) extends Constant
  case class ListConstant(elems: Seq[Constant]) extends Constant
  case class MapConstant(elems: Map[Constant, Constant]) extends Constant
  case class StringConstant(value: String) extends Constant
  case class Identifier(name: String) extends Constant
  case object NullConstant extends Constant

  sealed trait FunctionType
  case object Void extends FunctionType
  sealed trait FieldType extends FunctionType
  sealed trait BaseType extends FieldType
  case object TBool extends BaseType
  case object TByte extends BaseType
  case object TI16 extends BaseType
  case object TI32 extends BaseType
  case object TI64 extends BaseType
  case object TDouble extends BaseType
  case object TString extends BaseType
  case object TBinary extends BaseType

  trait NamedType extends FieldType {
    def name: String
  }

  case class ReferenceType(name: String) extends NamedType

  case class StructType(struct: StructLike) extends NamedType {
    def name = struct.name
  }

  case class EnumType(enum: Enum) extends NamedType {
    def name = enum.name
  }

  sealed abstract class ContainerType(cppType: Option[String]) extends FieldType
  case class MapType(keyType: FieldType, valueType: FieldType, cppType: Option[String]) extends ContainerType(cppType)
  case class SetType(eltType: FieldType, cppType: Option[String]) extends ContainerType(cppType)
  case class ListType(eltType: FieldType, cppType: Option[String]) extends ContainerType(cppType)

  case class Field(
    id: Int,
    name: String,
    `type`: FieldType,
    default: Option[Constant] = None,
    requiredness: Requiredness = Requiredness.Default)
  {
    def camelize = copy(name = camelCase(name))
  }

  case class Function(
    name: String,
    `type`: FunctionType,
    args: Seq[Field],
    oneway: Boolean,
    throws: Seq[Field])
  {
    def camelize = copy(name = camelCase(name))
  }

  sealed abstract class Definition {
    val name: String
    def camelize: Definition = this
  }

  case class Const(name: String, `type`: FieldType, value: Constant) extends Definition

  case class Typedef(name: String, `type`: FieldType) extends Definition

  case class Enum(name: String, values: Seq[EnumValue]) extends Definition

  case class EnumValue(name: String, value: Int)

  case class Senum(name: String, values: Seq[String]) extends Definition

  sealed abstract class StructLike extends Definition {
    val fields: Seq[Field]
  }

  case class Struct(name: String, fields: Seq[Field]) extends StructLike {
    override def camelize = copy(fields = fields.map(_.camelize))
  }

  case class Exception_(name: String, fields: Seq[Field]) extends StructLike {
    override def camelize = copy(fields = fields.map(_.camelize))
  }

  case class Service(name: String, parent: Option[String], functions: Seq[Function]) extends Definition {
    override def camelize = copy(functions = functions.map(_.camelize))
  }

  sealed abstract class Header
  case class Include(filename: String, document: Document) extends Header
  case class CppInclude(file: String) extends Header
  case class Namespace(scope: String, name: String) extends Header

  case class Document(headers: Seq[Header], defs: Seq[Definition]) {
    def camelize = copy(defs = defs.map(_.camelize))
  }

  implicit def camelCase(str: String) = {
    val sb = new StringBuilder(str.length)
    var up = false
    for (c <- str) {
      if (up) {
        sb.append(c.toUpper)
        up = false
      } else if (c == '_') {
        up = true
      } else {
        sb.append(c)
      }
    }
    sb.toString
  }
}
