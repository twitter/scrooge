package com.twitter.scrooge.swift_generator

import com.twitter.scrooge.ast.Identifier
import com.twitter.scrooge.ast.Enum
import com.twitter.scrooge.java_generator.TypeController

class EnumConstant(val name: String, val value: Int, val last: Boolean)

class EnumController(
  e: Enum,
  generator: SwiftGenerator,
  ns: Option[Identifier],
  objcPrefix: Option[String],
  val public_interface: Boolean)
    extends TypeController(e, generator, ns) {

  val is_objc: Boolean = objcPrefix.isDefined
  val objc_prefix: String = objcPrefix.getOrElse("")

  val alternative_name: String = e.annotations.getOrElse("alternative.type", name)

  val constants: Seq[EnumConstant] = e.values.zipWithIndex map {
    case (v, i) =>
      new EnumConstant(v.sid.toCamelCase.name, v.value, i == e.values.size - 1)
  }
}
