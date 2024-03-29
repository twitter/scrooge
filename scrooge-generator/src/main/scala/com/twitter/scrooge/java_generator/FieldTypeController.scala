package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast._
import com.twitter.scrooge.ast.ListType
import com.twitter.scrooge.frontend.ScroogeInternalException

class FieldTypeController(fieldType: FunctionType, generator: ApacheJavaGenerator) {
  def to_enum: String = generator.getTypeString(fieldType)
  def to_enum_compat: String = generator.getTypeStringWithEnumMapping(fieldType)
  val type_name: String = generator.typeName(fieldType)
  val type_name_in_container: String = generator.typeName(fieldType, inContainer = true)
  val type_name_in_container_skip_generic: String =
    generator.typeName(fieldType, inContainer = true, skipGeneric = true)
  val qualified_type_name: String =
    generator.typeName(fieldType, fileNamespace = Some(generator.namespace))
  val init_type_name: String = generator.typeName(fieldType, inInit = true)
  def is_enum_set: Boolean = fieldType match {
    case SetType(_: EnumType, _) => true
    case at: AnnotatedFieldType => new FieldTypeController(at.unwrap, generator).is_enum_set
    case _ => false
  }
  def init_field: String = generator.initField(fieldType)
  def init_container_field_prelude: String = generator.initContainerFieldPrelude(fieldType)
  val nullable: Boolean = generator.isNullableType(fieldType)
  val double: Boolean = fieldType match {
    case TDouble => true
    case at: AnnotatedFieldType => new FieldTypeController(at.unwrap, generator).double
    case _ => false
  }
  val boolean: Boolean = fieldType match {
    case TBool => true
    case at: AnnotatedFieldType => new FieldTypeController(at.unwrap, generator).boolean
    case _ => false
  }
  val is_container: Boolean = fieldType match {
    case _: MapType | _: SetType | _: ListType => true
    case at: AnnotatedFieldType => new FieldTypeController(at.unwrap, generator).is_container
    case _ => false
  }
  val is_map_or_set: Boolean = fieldType match {
    case _: MapType | _: SetType => true
    case at: AnnotatedFieldType => new FieldTypeController(at.unwrap, generator).is_map_or_set
    case _ => false
  }
  def is_preallocatable: Boolean = is_container && !is_enum_set
  val is_enum: Boolean = fieldType match {
    case _: EnumType => true
    case at: AnnotatedFieldType => new FieldTypeController(at.unwrap, generator).is_enum
    case _ => false
  }
  // is the field value effectively an i32
  val base_int_type: Boolean = !double && !boolean
  val is_list_or_set: Any = fieldType match {
    case SetType(x, _) => Map("elem_type" -> new FieldTypeController(x, generator))
    case ListType(x, _) => Map("elem_type" -> new FieldTypeController(x, generator))
    case at: AnnotatedFieldType => new FieldTypeController(at.unwrap, generator).is_list_or_set
    case _ => false
  }
  val is_list: Boolean = fieldType match {
    case _: ListType => true
    case at: AnnotatedFieldType => new FieldTypeController(at.unwrap, generator).is_list
    case _ => false
  }
  val is_map: Boolean = fieldType match {
    case _: MapType => true
    case at: AnnotatedFieldType => new FieldTypeController(at.unwrap, generator).is_map
    case _ => false
  }
  def map_types: Any = fieldType match {
    case MapType(k, v, _) =>
      Map(
        "key_type" -> new FieldTypeController(k, generator),
        "value_type" -> new FieldTypeController(v, generator)
      )
    case at: AnnotatedFieldType => new FieldTypeController(at.unwrap, generator).map_types
    case _ => false
  }
  val is_binary: Boolean = fieldType match {
    case TBinary => true
    case at: AnnotatedFieldType => new FieldTypeController(at.unwrap, generator).is_binary
    case _ => false
  }
  val is_typedef: Boolean = fieldType match {
    case _: Typedef => true
    case at: AnnotatedFieldType => new FieldTypeController(at.unwrap, generator).is_typedef
    case _ => false
  }
  val is_base_type: Boolean = fieldType match {
    case Void | TString | TBool | TByte | TI16 | TI32 | TI64 | TDouble => true
    case at: AnnotatedFieldType => new FieldTypeController(at.unwrap, generator).is_base_type
    case _ => false
  }
  val is_base_type_or_enum: Boolean = is_base_type || (fieldType match {
    case _: EnumType => true
    case at: AnnotatedFieldType =>
      new FieldTypeController(at.unwrap, generator).is_base_type_or_enum
    case _ => false
  })
  val is_base_type_or_binary: Boolean = is_base_type || is_binary
  val is_base_type_not_string: Boolean = is_base_type && (fieldType match {
    case TString => false
    case at: AnnotatedFieldType =>
      new FieldTypeController(at.unwrap, generator).is_base_type_not_string
    case _ => true
  })
  val is_struct: Boolean = fieldType match {
    case _: StructType => true // this can be a struct or an exception
    case at: AnnotatedFieldType => new FieldTypeController(at.unwrap, generator).is_struct
    case _ => false
  }
  val is_struct_or_enum: Boolean = is_struct || is_enum
  val is_void: Boolean = fieldType match {
    case Void | OnewayVoid => true
    case at: AnnotatedFieldType => new FieldTypeController(at.unwrap, generator).is_void
    case _ => false
  }
  val has_struct_at_leaf: Boolean = hasStructAtLeaf(fieldType)

  def get_type: String = {
    fieldType match {
      case at: AnnotatedFieldType => new FieldTypeController(at.unwrap, generator).get_type
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
          case at: AnnotatedFieldType => hasStructAtLeaf(at.unwrap)
          case StructType(_, _) => true
          case MapType(keyType, valueType, _) =>
            hasStructAtLeaf(keyType) || hasStructAtLeaf(valueType)
          case SetType(setType, _) => hasStructAtLeaf(setType)
          case ListType(listType, _) => hasStructAtLeaf(listType)
          case _ => false
        }
      case _ => false
    }
  }
}
