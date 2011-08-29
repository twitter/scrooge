package com.twitter.scrooge
package scalagen

import AST._
import org.monkey.mustache.Dictionary

trait StructTemplate extends Generator with ScalaTemplate { self: ScalaGenerator =>
  // ----- readers

  lazy val readBasicTemplate = handlebar[FieldType]("readBasic") { self =>
    Dictionary().data("protocolReadMethod", protocolReadMethod(self))
  }

  lazy val readListTemplate = handlebar[FieldType]("readList") {
    case self: ListType =>
      Dictionary()
        .data("eltType", scalaType(self.eltType))
        .data("eltReader", readTemplate(self.eltType).indent(2))
    case wtf => throw new InternalError("unexpected type: " + wtf)
  }

  lazy val readSetTemplate = handlebar[FieldType]("readSet") {
    case self: SetType =>
      Dictionary()
        .data("eltType", scalaType(self.eltType))
        .data("eltReader", readTemplate(self.eltType).indent(2))
    case wtf => throw new InternalError("unexpected type: " + wtf)
  }

  lazy val readMapTemplate = handlebar[FieldType]("readMap") {
    case self: MapType =>
      Dictionary()
        .data("keyType", scalaType(self.keyType))
        .data("valueType", scalaType(self.valueType))
        .data("keyReader", readTemplate(self.keyType).indent(2))
        .data("valueReader", readTemplate(self.valueType).indent(2))
    case wtf => throw new InternalError("unexpected type: " + wtf)
  }

  lazy val readStructTemplate = handlebar[FieldType]("readStruct") {
    case self: StructType =>
      Dictionary().data("name", self.name)
    case wtf => throw new InternalError("unexpected type: " + wtf)
  }

  lazy val readEnumTemplate = handlebar[FieldType]("readEnum") {
    case self: EnumType =>
      Dictionary().data("name", self.name)
    case wtf => throw new InternalError("unexpected type: " + wtf)
  }

  lazy val readTemplate: Handlebar[FieldType] = new Handlebar[FieldType] {
    def apply(t: FieldType) = {
      val template: Handlebar[FieldType] = t match {
        case _: ListType => readListTemplate
        case _: SetType => readSetTemplate
        case _: MapType => readMapTemplate
        case _: StructType => readStructTemplate
        case _: EnumType => readEnumTemplate
        case _: BaseType => readBasicTemplate
      }
      template(t)
    }
  }

  lazy val readFieldTemplate = handlebar[Field]("readField") { self =>
    Dictionary()
      .data("id", self.id.toString)
      .data("name", self.name)
      .data("constType", constType(self.`type`))
      .data("optionality", if (self.requiredness.isOptional) "Some" else "")
      .data("valueReader", readTemplate(self.`type`).indent(4))
      .bool("required", self.requiredness.isRequired)
  }

  // ----- writers

  lazy val writeBasicTemplate = handlebar[FieldType]("writeBasic") { self =>
    Dictionary().data("protocolWriteMethod", protocolWriteMethod(self))
  }

  lazy val writeListTemplate = handlebar[FieldType]("writeList") {
    case self: ListType =>
      Dictionary()
        .data("eltType", constType(self.eltType))
        .data("eltWriter", writeTemplate(self.eltType).indent(1))
    case wtf => throw new InternalError("unexpected type: " + wtf)
  }

  lazy val writeSetTemplate = handlebar[FieldType]("writeSet") {
    case self: SetType =>
      Dictionary()
        .data("eltType", constType(self.eltType))
        .data("eltWriter", writeTemplate(self.eltType).indent(1))
    case wtf => throw new InternalError("unexpected type: " + wtf)
  }

  lazy val writeMapTemplate = handlebar[FieldType]("writeMap") {
    case self: MapType =>
      Dictionary()
        .data("keyType", constType(self.keyType))
        .data("valueType", constType(self.valueType))
        .data("keyWriter", writeTemplate(self.keyType).indent(2))
        .data("valueWriter", writeTemplate(self.valueType).indent(2))
    case wtf => throw new InternalError("unexpected type: " + wtf)
  }

  lazy val writeStructTemplate = handlebar[FieldType]("writeStruct") {
    case self: StructType => Dictionary()
    case wtf => throw new InternalError("unexpected type: " + wtf)
  }

  lazy val writeEnumTemplate = handlebar[FieldType]("writeEnum") {
    case self: EnumType => Dictionary()
    case wtf => throw new InternalError("unexpected type: " + wtf)
  }

  lazy val writeTemplate: Handlebar[FieldType] = new Handlebar[FieldType] {
    def apply(self: FieldType): String = {
      val template = self match {
        case _: ListType => writeListTemplate
        case _: SetType => writeSetTemplate
        case _: MapType => writeMapTemplate
        case _: StructType => writeStructTemplate
        case _: EnumType => writeEnumTemplate
        case _: BaseType => writeBasicTemplate
      }
      template(self)
    }
  }

  lazy val writeFieldTemplate = handlebar[Field]("writeField") { self =>
    val conditional = if (self.requiredness.isOptional) {
      self.name + ".isDefined"
    } else {
      self.`type` match {
        case AST.TBool | AST.TByte | AST.TI16 | AST.TI32 | AST.TI64 | AST.TDouble =>
          "true"
        case _ =>
          self.name + " ne null"
      }
    }
    Dictionary()
      .data("name", self.name)
      .data("conditional", conditional)
      .data("fieldConst", writeFieldConst(self.name))
      .data("getter", if (self.requiredness.isOptional) ".get" else "")
      .data("valueWriter", writeTemplate(self.`type`).indent(1))
  }

  // ----- struct

  lazy val structTemplate = handlebar[StructLike]("struct"){ struct =>
    val fieldDictionaries = struct.fields map { field =>
      Dictionary()
        .data("name", field.name)
        .data("id", field.id.toString)
        .data("fieldConst", writeFieldConst(field.name))
        .data("constType", constType(field.`type`))
        .data("scalaType", scalaFieldType(field))
        .data("defaultReadValue", defaultReadValue(field))
        .bool("required", field.requiredness.isRequired)
        .data("reader", readFieldTemplate(field).indent(5))
        .data("writer", writeFieldTemplate(field).indent(2))
        .data("struct", struct.name)
    }
    val optionalDefaultDictionaries = struct.fields.filter { f =>
      f.requiredness.isOptional && f.default.isDefined
    } map { f =>
      Dictionary()
        .data("name", f.name)
        .data("value", constantValue(f.default.get))
    }
    val parentType = struct match {
      case AST.Struct(_, _) => "ThriftStruct"
      case AST.Exception_(_, _) => "Exception with ThriftStruct"
    }
    Dictionary()
      .data("name", struct.name)
      .data("fieldNames", struct.fields.map { f => f.name }.mkString(", "))
      .data("fieldArgs", fieldArgs(struct.fields))
      .data("parentType", parentType)
      .dictionaries("fields", fieldDictionaries)
      .dictionaries("optionalDefaults", optionalDefaultDictionaries)
  }
}
