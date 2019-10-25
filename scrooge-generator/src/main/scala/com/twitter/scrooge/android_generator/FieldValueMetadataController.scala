package com.twitter.scrooge.android_generator

import com.twitter.scrooge.ast._
import com.twitter.scrooge.java_generator.{
  FieldValueMetadataController => JavaFieldValueMetadataController
}

class FieldValueMetadataController(
  fieldType: FieldType,
  generator: AndroidGenerator,
  ns: Option[Identifier])
    extends JavaFieldValueMetadataController(fieldType, generator, ns) {
  override val field_type: FieldTypeController = new FieldTypeController(fieldType, generator)
}
