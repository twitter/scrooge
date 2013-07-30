package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast.{Identifier, Enum}

class EnumConstant(val name: String, val value: Int, val last: Boolean)

class EnumController(e: Enum, generator: ApacheJavaGenerator, ns: Option[Identifier])
  extends BaseController(generator, ns) {
  val name = e.sid.name
  val constants = e.values.zipWithIndex map { case (v, i) =>
    new EnumConstant(v.sid.name, v.value, i == e.values.size - 1)
  }
}

