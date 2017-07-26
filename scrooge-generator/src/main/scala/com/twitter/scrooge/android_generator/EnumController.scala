package com.twitter.scrooge.android_generator

import com.twitter.scrooge.ast.{Identifier, Enum}
import com.twitter.scrooge.java_generator.TypeController

class EnumConstant(val name: String, val value: Int, val last: Boolean)

class EnumController(e: Enum, generator: AndroidGenerator, ns: Option[Identifier])
    extends TypeController(e, generator, ns) {
  val constants = e.values.zipWithIndex map {
    case (v, i) =>
      new EnumConstant(v.sid.name, v.value, i == e.values.size - 1)
  }
}
