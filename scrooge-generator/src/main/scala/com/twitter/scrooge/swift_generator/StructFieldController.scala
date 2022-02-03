package com.twitter.scrooge.swift_generator

import com.twitter.scrooge.ast.EnumType
import com.twitter.scrooge.ast.Field
import com.twitter.scrooge.ast.FieldType
import com.twitter.scrooge.ast.Identifier
import com.twitter.scrooge.ast.StructType
import com.twitter.scrooge.java_generator.BaseController

class StructFieldController(
  f: Field,
  val i: Int,
  total: Int,
  generator: SwiftGenerator,
  ns: Option[Identifier],
  serializePrefix: String)
    extends BaseController(generator, ns) {
  val field: FieldController = new FieldController(f, generator, ns)
  val fieldType: FieldType = f.fieldType
  val field_type: FieldTypeController = field.field_type
  val optional_or_nullable: Boolean = field.optional || field_type.nullable
  val name: String = f.sid.toCamelCase.name
  def alternative_type_name: String = {
    fieldType match {
      case s: StructType => s.struct.annotations.get("alternative.type")
      case e: EnumType => e.`enum`.annotations.get("alternative.type")
      case _ => None
    }
  }.getOrElse(field_type.type_name_in_container)

  val key: Int = f.index

  def print_const: String =
    indent(
      generator.printConstValue(
        "self." + name,
        fieldType,
        f.default.get,
        ns,
        in_static = true,
        defval = true
      ),
      4
    )
  def last: Boolean = i == total - 1
  def first: Boolean = i == 0
}
