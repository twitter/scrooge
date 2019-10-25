package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast._

class BaseSerializeController(
  fieldType: FieldType,
  fieldName: String,
  prefix: String,
  generator: ApacheJavaGenerator,
  ns: Option[Identifier])
    extends BaseController(generator, ns) {
  val field_type: FieldTypeController = new FieldTypeController(fieldType, generator)
  val name: String = prefix + fieldName
}
