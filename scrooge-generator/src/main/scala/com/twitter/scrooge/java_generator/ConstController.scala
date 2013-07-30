package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast.{Identifier, ConstDefinition}

/**
 * Generate a class that holds all the constants
 */
class ConstController(defs: Seq[ConstDefinition], generator: ApacheJavaGenerator, ns: Option[Identifier])
  extends BaseController(generator, ns) {
  val constants = defs map { d =>
    Map("rendered_value" -> indent(generator.printConstValue(d.sid.name, d.fieldType, d.value, ns), 2))
  }
}
