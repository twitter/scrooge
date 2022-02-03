package com.twitter.scrooge.swift_generator

import com.twitter.scrooge.ast.Identifier
import com.twitter.scrooge.ast.Field
import com.twitter.scrooge.java_generator.{FieldController => JavaFieldController}

class FieldController(f: Field, generator: SwiftGenerator, ns: Option[Identifier])
    extends JavaFieldController(f, generator, ns) {

  override val field_type: FieldTypeController = new FieldTypeController(f.fieldType, generator)
}
