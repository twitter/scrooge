package com.twitter.scrooge.android_generator

import com.twitter.scrooge.ast.{ConstDefinition, Identifier}
import com.twitter.scrooge.java_generator.{TypeController}

/**
 * Helps generate a class that holds all the constants.
 */
class ConstController(defs: Seq[ConstDefinition], generator: AndroidGenerator, ns: Option[Identifier])
  extends TypeController("Constants", generator, ns) {
  val constants = defs map { d =>
    Map("rendered_value" -> indent(generator.printConstValue(d.sid.name, d.fieldType, d.value, ns), 2))
  }
}
