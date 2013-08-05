package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast._
import com.twitter.scrooge.ast.ListType
import com.twitter.scrooge.frontend.ScroogeInternalException

class FieldTypeController(fieldType: FunctionType, generator: ApacheJavaGenerator) {
  def to_enum = generator.getTypeString(fieldType)
  val type_name = generator.typeName(fieldType)
  val type_name_in_container = generator.typeName(fieldType, inContainer = true)
  val type_name_in_container_skip_generic = generator.typeName(fieldType, inContainer = true, skipGeneric = true)
  val init_type_name = generator.typeName(fieldType, inInit = true)
  val nullable = generator.isNullableType(fieldType)
  val double = fieldType == TDouble
  val boolean = fieldType == TBool
  val is_container = fieldType match {
    case _: MapType | _: SetType | _: ListType => true
    case _ => false
  }
  val is_enum = fieldType.isInstanceOf[EnumType]
  // is the field value effectively an i32
  val base_int_type = fieldType != TDouble && fieldType != TBool
  val is_list_or_set = fieldType match {
    case SetType(x, _) => Map("elem_type" -> new FieldTypeController(x, generator))
    case ListType(x, _) => Map("elem_type" -> new FieldTypeController(x, generator))
    case _ => false
  }
  val is_list = fieldType.isInstanceOf[ListType]
  val is_map = fieldType.isInstanceOf[MapType]
  def map_types = fieldType match {
    case MapType(k, v, _) =>
      Map("key_type" -> new FieldTypeController(k, generator), "value_type" -> new FieldTypeController(v, generator))
    case _ => false
  }
  val is_binary = fieldType == TBinary
  val is_typedef = fieldType.isInstanceOf[Typedef]
  val is_base_type = fieldType match {
    case Void | TString | TBool | TByte | TI16 | TI32 | TI64 | TDouble => true
    case _ => false
  }
  val is_base_type_or_enum = is_base_type || fieldType.isInstanceOf[EnumType]
  val is_base_type_or_binary = is_base_type || fieldType == TBinary
  val is_base_type_not_string = is_base_type && fieldType != TString
  val is_struct = fieldType.isInstanceOf[StructType] // this can be a struct or an exception
  val is_struct_not_exception = fieldType match {
    case StructType(s, _) => !s.isInstanceOf[Exception_]
    case _ => false
  }

  val is_struct_or_enum = is_struct_not_exception || fieldType.isInstanceOf[EnumType]
  val is_void = fieldType == Void

  def get_type = {
    fieldType match {
      case TString => "String"
      case TBool => "Bool"
      case TByte => "Byte"
      case TI16 => "I16"
      case TI32 => "I32"
      case TI64 => "I64"
      case TDouble => "Double"
      case TBinary => "Binary"
      case MapType(key, value, cpp) => "Map"
      case SetType(key, cpp) => "Set"
      case ListType(key, cpp) => "List"
      case _ => {
        throw new ScroogeInternalException("InvalidType for base type: " + fieldType)
      }
    }
  }
}
