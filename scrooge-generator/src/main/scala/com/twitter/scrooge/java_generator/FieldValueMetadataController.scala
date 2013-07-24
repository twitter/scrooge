package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast._
import com.twitter.scrooge.ast.SetType
import com.twitter.scrooge.ast.MapType

class FieldValueMetadataController(
    fieldType: FieldType,
    generator: ApacheJavaGenerator,
    ns: Option[Identifier])
  extends BaseController(generator, ns) {
  val field_type = new FieldTypeController(fieldType, generator)

  def map_element = {
    fieldType match {
      case MapType(k, v, _) => {
        Map(
          "field_value_meta_data_key" -> generateMetadata(k),
          "field_value_meta_data_val" -> generateMetadata(v)
        )
      }
      case _ => false
    }
  }

  def set_or_list_element = {
    fieldType match {
      case SetType(x, _) => elem(x)
      case ListType(x, _) => elem(x)
      case _ => false
    }
  }

  def elem(x: FieldType): Map[String, Object] = {
    Map("field_value_meta_data_elem" -> generateMetadata(x))
  }

  def generateMetadata(k: FieldType): String = {
    indent(generator.fieldValueMetaData(k, ns), 4, skipFirst = true, addLast = false)
  }
}
