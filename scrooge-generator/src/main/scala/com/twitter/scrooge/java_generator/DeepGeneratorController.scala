package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast._

sealed trait DeepGeneratorOperation {
  def name: String
  def containerMustacheFileName: String
  def nonContainerMustacheFileName: String
}

object Copy extends DeepGeneratorOperation {
  override val name: String = "copy"
  override val containerMustacheFileName: String = "generate_deep_copy_container.mustache"
  override val nonContainerMustacheFileName: String = "generate_deep_copy_noncontainer.mustache"
}

object Validate extends DeepGeneratorOperation {
  override val name: String = "validate"
  override val containerMustacheFileName: String = "generate_deep_validate_container.mustache"
  override val nonContainerMustacheFileName: String = "generate_deep_validate_noncontainer.mustache"
}

class DeepGeneratorController(
  sourceNamePart1: String,
  sourceNamePart2: Option[String],
  val result_name: String,
  fieldType: FieldType,
  generator: ApacheJavaGenerator,
  ns: Option[Identifier],
  operation: DeepGeneratorOperation)
    extends BaseController(generator, ns) {
  val source_name: String =
    sourceNamePart2.map(sourceNamePart1 + "." + _).getOrElse(sourceNamePart1)
  val iterator_element_name: String = sourceNamePart1 + "_element"
  val result_element_name: String = s"${result_name}_${operation.name}"
  val field_type: FieldTypeController = new FieldTypeController(fieldType, generator)
  val direct_copy: Boolean =
    field_type.is_base_type || field_type.is_enum || field_type.is_typedef || field_type.is_binary

  def map_value: Any = {
    fieldType match {
      case at: AnnotatedFieldType => unwrap(at).map_value
      case MapType(k, v, _) =>
        Map(
          "key_type" -> new FieldTypeController(k, generator),
          "val_type" -> new FieldTypeController(v, generator),
          s"generate_deep_${operation.name}_key_in_container" -> deepContainer("_key", k),
          s"generate_deep_${operation.name}_key_non_container" -> deepNonContainer("_key", k),
          s"generate_deep_${operation.name}_val_in_container" -> deepContainer("_value", v),
          s"generate_deep_${operation.name}_val_non_container" -> deepNonContainer("_value", v)
        )
      case _ => false
    }
  }

  def deepNonContainer(suffix: String, k: FieldType): String = {
    generator.deepNonContainer(iterator_element_name + suffix, k, ns, operation)
  }

  def deepContainer(suffix: String, k: FieldType): String = {
    val deepCopy = generator.deepContainer(
      iterator_element_name + suffix,
      None,
      result_element_name + suffix,
      k,
      ns,
      operation
    )
    indent(deepCopy, 2, true, true)
  }

  def list_or_set_value: Any = {
    fieldType match {
      case at: AnnotatedFieldType => unwrap(at).list_or_set_value
      case SetType(x, _) => getListSetMap(x)
      case ListType(x, _) => getListSetMap(x)
      case _ => false
    }
  }

  def getListSetMap(x: FieldType): Map[String, Any] = {
    val xFieldType = new FieldTypeController(x, generator)
    Map(
      "elem_type" -> xFieldType,
      s"generate_deep_${operation.name}_in_container" -> deepContainer("", x),
      s"generate_deep_${operation.name}_non_container" -> deepNonContainer("", x)
    )
  }

  private def unwrap(at: AnnotatedFieldType): DeepGeneratorController =
    new DeepGeneratorController(
      sourceNamePart1,
      sourceNamePart2,
      result_name,
      at.unwrap,
      generator,
      ns,
      operation)
}
