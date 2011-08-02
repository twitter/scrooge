package com.twitter.scrooge

object AST {
  sealed abstract class Requiredness
  object Requiredness {
    case object Optional extends Requiredness
    case object Required extends Requiredness
    case object Default extends Requiredness
  }

  sealed abstract class Constant
  case class BoolConstant(value: Boolean) extends Constant
  case class IntConstant(value: Long) extends Constant
  case class DoubleConstant(value: Double) extends Constant
  case class ListConstant(elems: Array[Constant]) extends Constant
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

  sealed abstract class ContainerType(cppType: Option[String]) extends DefinitionType
  case class MapType(keyType: FieldType, valueType: FieldType, cppType: Option[String]) extends ContainerType(cppType)
  case class SetType(tpe: FieldType, cppType: Option[String]) extends ContainerType(cppType)
  case class ListType(tpe: FieldType, cppType: Option[String]) extends ContainerType(cppType)

  case class Field(id: Int, name: String, `type`: FieldType, default: Option[Constant],
    requiredness: Requiredness) {
    def camelize = copy(name = camelCase(name))
  }

  case class Function(name: String, `type`: FunctionType, args: Array[Field], oneway: Boolean,
    throws: Array[Field]) {
    def camelize = copy(name = camelCase(name))
    override def equals(other: Any) = {
      other match {
        case Function(oName, oType, oArgs, oOneWay, oThrows) => {
          name == oName &&
          `type` == oType &&
          args.toList == oArgs.toList &&
          oneway == oOneWay &&
          throws.toList == oThrows.toList
        }
        case _ => false
      }
    }
  }

  sealed abstract class Definition {
    val name: String
    def camelize: Definition = this
  }

  case class Const(name: String, `type`: FieldType, value: Constant) extends Definition

  case class Typedef(name: String, `type`: DefinitionType) extends Definition

  case class Enum(name: String, values: Array[EnumValue]) extends Definition {
    override def equals(other: Any) = {
      other match {
        case Enum(oName, oValues) => {
          name == oName &&
          values.toList == oValues.toList
        }
        case _ => false
      }
    }
  }

  case class EnumValue(name: String, value: Int)

  case class Senum(name: String, values: Array[String]) extends Definition {
    override def equals(other: Any) = {
      other match {
        case Senum(oName, oValues) => {
          name == oName &&
          values.toList == oValues.toList
        }
        case _ => false
      }
    }
  }

  sealed abstract class StructLike extends Definition {
    val fields: Array[Field]
  }

  case class Struct(name: String, fields: Array[Field]) extends StructLike {
    override def camelize = copy(fields = fields.map(_.camelize))
    override def equals(other: Any) = {
      other match {
        case Struct(oName, oFields) => name == oName && fields.toList == oFields.toList
        case _ => false
      }
    }
  }

  case class Exception_(name: String, fields: Array[Field]) extends StructLike {
    override def camelize = copy(fields = fields.map(_.camelize))
    override def equals(other: Any) = {
      other match {
        case Exception_(oName, oFields) => name == oName && fields.toList == oFields.toList
        case _ => false
      }
    }
  }

  case class Service(name: String, parent: Option[String], functions: Array[Function]) extends Definition {
    override def camelize = copy(functions = functions.map(_.camelize))
    override def equals(other: Any) = {
      other match {
        case Service(oName, oParent, oFunctions) => name == oName && parent == oParent && functions.toList == oFunctions.toList
        case _ => false
      }
    }
  }

  sealed abstract class Header
  case class Include(filename: String, document: Document) extends Header
  case class CppInclude(file: String) extends Header
  case class Namespace(scope: String, name: String) extends Header

  case class Document(headers: Array[Header], defs: Array[Definition]) {
    def camelize = copy(defs = defs.map(_.camelize))
    override def equals(other: Any) = {
      other match {
        case Document(oHeaders, oDefs) => headers.toList == oHeaders.toList && defs.toList == oDefs.toList
        case _ => false
      }
    }
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
