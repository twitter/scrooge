package com.twitter.scrooge.android_generator

import com.twitter.scrooge.ast.{Identifier, Field}
import com.twitter.scrooge.java_generator.{FieldController => JavaFieldController}

class FieldController(f: Field, generator: AndroidGenerator, ns: Option[Identifier])
    extends JavaFieldController(f, generator, ns) {

  override val field_type: FieldTypeController = new FieldTypeController(f.fieldType, generator)
}
