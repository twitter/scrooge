package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast._
import com.twitter.scrooge.ast.ListType
import com.twitter.scrooge.frontend.ScroogeInternalException

class FieldTypeController(
  fieldType: FunctionType,
  generator: ApacheJavaGenerator
) {
  def to_enum = generator.getTypeString(fieldType)
  def to_enum_compat = generator.getTypeStringWithEnumMapping(fieldType)
  val type_name = generator.typeName(fieldType)
  val type_name_in_container = generator.typeName(fieldType, inContainer = true)
  val type_name_in_container_skip_generic =
    generator.typeName(fieldType, inContainer = true, skipGeneric = true)
  val qualified_type_name = generator.typeName(fieldType, fileNamespace = Some(generator.namespace))
  val init_type_name = generator.typeName(fieldType, inInit = true)
  def is_enum_set: Boolean = fieldType match {
    case SetType(_: EnumType, _) => true
    case _ => false
  }
  def init_field = generator.initField(fieldType)
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
      Map(
        "key_type" -> new FieldTypeController(k, generator),
        "value_type" -> new FieldTypeController(v, generator)
      )
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
  val is_struct_or_enum = is_struct || fieldType.isInstanceOf[EnumType]
  val is_void: Boolean = fieldType == Void || fieldType == OnewayVoid
  val has_struct_at_leaf = hasStructAtLeaf(fieldType)

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

  /**
   * Recursive method for finding out whether a type has a struct at it's leaf. This is used for
   * validateNewInstance. It tries only needs to generate the code for validation of deep
   * structures when the there are structs at the leafs.
   */
  private[this] def hasStructAtLeaf(functionType: FunctionType): Boolean = {
    functionType match {
      case fieldType: FieldType =>
        fieldType match {
          case StructType(_, _) => true
          case MapType(keyType, valueType, _) => hasStructAtLeaf(keyType) || hasStructAtLeaf(valueType)
          case SetType(setType, _) => hasStructAtLeaf(setType)
          case ListType(listType, _) => hasStructAtLeaf(listType)
          case _ => false
        }
      case _ => false
    }
  }
}
