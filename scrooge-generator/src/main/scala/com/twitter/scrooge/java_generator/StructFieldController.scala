package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast.{Field, Identifier}
import com.twitter.scrooge.ast.FieldType

class StructFieldController(
  f: Field,
  val i: Int,
  total: Int,
  generator: ApacheJavaGenerator,
  ns: Option[Identifier],
  serializePrefix: String)
    extends BaseController(generator, ns) {
  val field: FieldController = new FieldController(f, generator, ns)
  val fieldType: FieldType = f.fieldType
  val field_type: FieldTypeController = field.field_type
  val optional_or_nullable: Boolean = field.optional || field_type.nullable
  val name: String = f.sid.name
  val deepCopyIndentLevel: Int = if (field_type.nullable) 6 else 4

  val generate_deep_copy_container: String =
    indent(generator.deepContainer("other", Some(name), "__this__" + name, fieldType, ns, Copy), 4)
  val generate_deep_copy_non_container: String =
    generator.deepNonContainer("other." + name, fieldType, ns, Copy)

  val generate_deep_validate_container: String =
    indent(generator.deepContainer("_" + name, None, "__this__" + name, fieldType, ns, Validate), 4)
  val generate_deep_validate_non_container: String =
    generator.deepNonContainer("_" + name, fieldType, ns, Validate)

  val key: Int = f.index
  val field_metadata: String =
    indent(generator.fieldValueMetaData(fieldType, ns), 6, addLast = false)
  def deserialize_field: String =
    indent(generator.deserializeField(fieldType, name, ns, serializePrefix), 12)
  def serialize_field: String =
    indent(generator.serializeField(fieldType, name, ns, serializePrefix), 4)
  def print_const: String =
    indent(
      generator.printConstValue(
        "this." + name,
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
