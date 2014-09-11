package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast.{Definition, Identifier}

/**
 * Helps generate a top-level java class.
 */
abstract class TypeController(val name: String, generator: ApacheJavaGenerator, ns: Option[Identifier])
  extends BaseController(generator, ns) {
  def this(typeId: Definition, generator: ApacheJavaGenerator, ns: Option[Identifier]) = {
    this(typeId.sid.name, generator, ns)
  }
}

