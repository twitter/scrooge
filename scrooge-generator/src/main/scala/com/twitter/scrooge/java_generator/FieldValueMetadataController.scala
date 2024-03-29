package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast._
import com.twitter.scrooge.ast.SetType
import com.twitter.scrooge.ast.MapType

class FieldValueMetadataController(
  fieldType: FieldType,
  generator: ApacheJavaGenerator,
  ns: Option[Identifier])
    extends BaseController(generator, ns) {
  val field_type: FieldTypeController = new FieldTypeController(fieldType, generator)

  def map_element: Any = {
    fieldType match {
      case at: AnnotatedFieldType => unwrap(at).map_element
      case MapType(k, v, _) => {
        Map(
          "field_value_meta_data_key" -> generateMetadata(k),
          "field_value_meta_data_val" -> generateMetadata(v)
        )
      }
      case _ => false
    }
  }

  def set_or_list_element: Any = {
    fieldType match {
      case at: AnnotatedFieldType => unwrap(at).set_or_list_element
      case SetType(x, _) => elem(x)
      case ListType(x, _) => elem(x)
      case _ => false
    }
  }

  def elem(x: FieldType): Map[String, Object] = {
    Map("field_value_meta_data_elem" -> generateMetadata(x))
  }

  def generateMetadata(k: FieldType): String = {
    k match {
      case at: AnnotatedFieldType => generateMetadata(at.unwrap)
      case otherwise =>
        indent(generator.fieldValueMetaData(otherwise, ns), 4, skipFirst = true, addLast = false)
    }
  }
  private def unwrap(at: AnnotatedFieldType): FieldValueMetadataController =
    new FieldValueMetadataController(at.unwrap, generator, ns)

}
