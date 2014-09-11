package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast._
import com.twitter.scrooge.ast.SetType
import com.twitter.scrooge.ast.MapType

class SerializeFieldController(
    fieldType: FieldType,
    fieldName: String,
    prefix: String,
    generator: ApacheJavaGenerator,
    ns: Option[Identifier])
  extends BaseSerializeController(fieldType, fieldName, prefix, generator, ns) {
  val tmp_iter = if (field_type.is_container) generator.tmp("_iter") else false

  def deserialize_map_element = {
    fieldType match {
      case MapType(k, v, _) => {
        Map(
          "key_type" -> new FieldTypeController(k, generator),
          "val_type" -> new FieldTypeController(v, generator),
          "serialize_key" -> indent(generator.serializeField(k, tmp_iter + ".getKey()", ns), 4),
          "serialize_val" -> indent(generator.serializeField(v, tmp_iter + ".getValue()", ns), 4)
        )
      }
      case _ => false
    }
  }

  def deserialize_set_or_list_element = {
    fieldType match {
      case SetType(x, _) => deserialize_elem(x)
      case ListType(x, _) => deserialize_elem(x)
      case _ => false
    }
  }

  def deserialize_elem(x: FieldType): Map[String, Object] = {
    Map(
      "elem_type" -> new FieldTypeController(x, generator),
      "serialize_elem" -> indent(generator.serializeField(x, tmp_iter.toString, ns), 2)
    )
  }
}
