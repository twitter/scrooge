package com.twitter.scrooge.swift_generator

import com.twitter.scrooge.ast.ConstDefinition
import com.twitter.scrooge.ast.EnumType
import com.twitter.scrooge.ast.FieldType
import com.twitter.scrooge.ast.Identifier
import com.twitter.scrooge.ast.ListRHS
import com.twitter.scrooge.ast.MapRHS
import com.twitter.scrooge.ast.SetRHS
import com.twitter.scrooge.java_generator.TypeController

/**
 * Helps generate a class that holds all the constants.
 */
class ConstController(
  defs: Seq[ConstDefinition],
  generator: SwiftGenerator,
  ns: Option[Identifier],
  val public_interface: Boolean)
    extends TypeController("Constants", generator, ns) {
  val constants: Seq[Map[String, String]] = defs map { d =>
    val size = d.value match {
      case map: MapRHS => map.elems.size
      case set: SetRHS => set.elems.size
      case list: ListRHS => list.elems.size
      case _ => 0
    }
    val camelCasedFieldTypes: FieldType = d.fieldType match {
      case e: EnumType =>
        val enum = e.`enum`
        e.copy(`enum`.copy(values = enum.values.map({ v => v.copy(v.sid.toCamelCase) })))
      case _ => d.fieldType
    }
    Map(
      "rendered_value" -> indent(
        generator.printConstValue(d.sid.name, camelCasedFieldTypes, d.value, ns, size, true),
        2)
    )
  }
}
