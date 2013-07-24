package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast._

class DeepCopyController(
    source_name_p1: String,
    source_name_p2: String,
    val result_name: String,
    fieldType: FieldType,
    generator: ApacheJavaGenerator,
    ns: Option[Identifier])
  extends BaseController(generator, ns) {
  val source_name = if (source_name_p2.isEmpty) source_name_p1 else source_name_p1 + "." + source_name_p2
  val iterator_element_name = source_name_p1 + "_element"
  val result_element_name = result_name + "_copy"
  val field_type = new FieldTypeController(fieldType, generator)
  val direct_copy = field_type.is_base_type || field_type.is_enum || field_type.is_typedef || field_type.is_binary

  def map_value = {
    fieldType match {
      case MapType(k, v, _) =>
        Map(
        "key_type" -> new FieldTypeController(k, generator),
        "val_type" -> new FieldTypeController(v, generator),
        "generate_deep_copy_key_in_container" -> deepContainerCopy("_key", k),
        "generate_deep_copy_key_non_container" -> deepNonContainerCopy("_key", k),
        "generate_deep_copy_val_in_container" -> deepContainerCopy("_value", v),
        "generate_deep_copy_val_non_container" -> deepNonContainerCopy("_value", v)
        )
      case _ => false
    }
  }

  def deepNonContainerCopy(suffix: String, k: FieldType): String = {
    generator.deepCopyNonContainer(iterator_element_name + suffix, k, ns)
  }

  def deepContainerCopy(suffix: String, k: FieldType): String = {
    val deepCopy = generator.deepCopyContainer(iterator_element_name + suffix, "", result_element_name + suffix, k, ns)
    indent(deepCopy, 2, true, true)
  }

  def list_or_set_value = {
    fieldType match {
      case SetType(x, _) => getListSetMap(x)
      case ListType(x, _) => getListSetMap(x)
      case _ => false
    }
  }

  def getListSetMap(x: FieldType): Map[String, Any] = {
    val xFieldType = new FieldTypeController(x, generator)
    Map(
      "elem_type" -> xFieldType,
      "generate_deep_copy_in_container" -> deepContainerCopy("", x),
      "generate_deep_copy_non_container" -> deepNonContainerCopy("", x)
    )
  }
}
