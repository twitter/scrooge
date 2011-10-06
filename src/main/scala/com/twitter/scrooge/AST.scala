package com.twitter.scrooge

import java.io.File

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

  sealed abstract class Constant {
    def camelize = this
  }
  case class BoolConstant(value: Boolean) extends Constant
  case class IntConstant(value: Long) extends Constant
  case class DoubleConstant(value: Double) extends Constant
  case class ListConstant(elems: Seq[Constant]) extends Constant
  case class MapConstant(elems: Map[Constant, Constant]) extends Constant
  case class StringConstant(value: String) extends Constant
  case class Identifier(name: String) extends Constant {
    override lazy val camelize = copy(name = TitleCase(name))
  }
  case class EnumValueConstant(enum: Enum, value: EnumValue) extends Constant {
    override lazy val camelize = copy(enum = enum.camelize, value = value.camelize)
  }
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
    lazy val camelize = copy(name = CamelCase(name), default = default.map(_.camelize))
  }

  case class Function(
    name: String,
    localName: String,
    `type`: FunctionType,
    args: Seq[Field],
    oneway: Boolean,
    throws: Seq[Field])
  {
    lazy val camelize = copy(
      localName = CamelCase(localName),
      args = args.map(_.camelize),
      throws = throws.map(_.camelize))
  }

  object Function {
    def apply(
      name: String,
      `type`: FunctionType,
      args: Seq[Field],
      oneway: Boolean,
      throws: Seq[Field]
    ) = {
      new Function(name, name, `type`, args, oneway, throws)
    }
  }

  sealed abstract class Definition {
    val name: String
    def camelize: Definition = this
  }

  case class Const(name: String, `type`: FieldType, value: Constant) extends Definition {
    override lazy val camelize = copy(value = value.camelize)
  }

  case class Typedef(name: String, `type`: FieldType) extends Definition

  case class Enum(name: String, values: Seq[EnumValue]) extends Definition {
    override lazy val camelize = copy(values = values.map(_.camelize))
  }

  case class EnumValue(name: String, value: Int) {
    lazy val camelize = copy(name = TitleCase(name))
  }

  case class Senum(name: String, values: Seq[String]) extends Definition

  sealed abstract class StructLike extends Definition {
    val fields: Seq[Field]
  }

  case class Struct(name: String, fields: Seq[Field]) extends StructLike {
    override lazy val camelize = copy(fields = fields.map(_.camelize))
  }

  case class Exception_(name: String, fields: Seq[Field]) extends StructLike {
    override lazy val camelize = copy(fields = fields.map(_.camelize))
  }

  object ServiceParent {
    def apply(service: Service): ServiceParent = ServiceParent(service.name, Some(service))
  }

  case class ServiceParent(name: String, service: Option[Service] = None)

  case class Service(
    name: String,
    parent: Option[ServiceParent],
    functions: Seq[Function]) extends Definition
  {
    override lazy val camelize = copy(functions = functions.map(_.camelize))
  }

  sealed abstract class Header

  case class Include(filename: String, document: Document) extends Header {
    lazy val prefix = stripExtension(filename)
  }

  case class CppInclude(file: String) extends Header

  case class Namespace(scope: String, name: String) extends Header

  case class Document(headers: Seq[Header], defs: Seq[Definition]) {
    def camelize = copy(defs = defs.map(_.camelize))

    lazy val scalaNamespace = {
      val scala = headers.collect { case Namespace("scala", x) => x }.headOption
      val java = headers.collect { case Namespace("java", x) => x }.headOption
      (scala orElse java).getOrElse("thrift")
    }

    def mapNamespaces(namespaceMap: Map[String,String]): Document = {
      copy(
        headers = headers map {
          case header @ Namespace(_, ns) =>
            namespaceMap.get(ns) map {
              newNs => header.copy(name = newNs)
            } getOrElse(header)
          case header => header
        }
      )
    }

    def consts = defs.collect { case c: Const => c }
    def enums = defs.collect { case e: Enum => e }
    def structs = defs.collect { case s: StructLike => s }
    def services = defs.collect { case s: Service => s }
  }

  def stripExtension(filename: String) = {
    filename.indexOf('.') match {
      case -1 => filename
      case dot => filename.substring(0, dot)
    }
  }
}
