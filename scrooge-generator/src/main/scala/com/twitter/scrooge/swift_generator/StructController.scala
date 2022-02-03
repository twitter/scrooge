package com.twitter.scrooge.swift_generator

import com.twitter.scrooge.ast._
import com.twitter.scrooge.ast.Field
import com.twitter.scrooge.java_generator.{TypeController => JavaTypeController}

class StructController(
  struct: StructLike,
  val in_class: Boolean,
  generator: SwiftGenerator,
  ns: Option[Identifier],
  val is_result: Boolean = false,
  objcPrefix: Option[String],
  use_classes: Boolean,
  val public_interface: Boolean)
    extends JavaTypeController(struct, generator, ns) {
  val struct_type_name: String = generator.typeName(StructType(struct))
  val alternative_name: String = struct.annotations.getOrElse("alternative.type", struct_type_name)
  val is_exception: Boolean = struct.isInstanceOf[Exception_]
  val is_union: Boolean = struct.isInstanceOf[Union]
  val allFields: Seq[Field] = struct.fields

  def cleanup(fields: Seq[Field]): Seq[StructFieldController] = {
    fields.zipWithIndex map {
      case (f, i) =>
        val serializePrefix = if (is_union) "" else "this."
        new StructFieldController(f, i, fields.size, generator, ns, serializePrefix)
    }
  }
  val fields: Seq[StructFieldController] = cleanup(allFields)
  val has_fields: Boolean = fields.size > 0
  val sorted_fields: Seq[StructFieldController] = cleanup(allFields.sortBy(f => f.index))

  val is_objc: Boolean = objcPrefix.isDefined
  val objc_prefix: String = objcPrefix.getOrElse("")
  val generate_classes: Boolean = objcPrefix.isDefined || use_classes

  val non_nullable_fields: Seq[StructFieldController] = cleanup(allFields.filter { f =>
    !generator.isNullableType(f.fieldType)
  })

  val has_non_nullable_fields: Boolean = non_nullable_fields.size > 0

  val has_bit_vector: Boolean = non_nullable_fields.size > 0

  val default_fields: Seq[StructFieldController] = cleanup(allFields.filter { f =>
    !f.default.isEmpty
  })

  val non_optional_fields: Seq[StructFieldController] = cleanup(allFields.filter { f =>
    !f.requiredness.isOptional
  })
  val non_default_constructor: Boolean = non_optional_fields.size > 0

  val has_list_or_set_fields: Boolean = cleanup(allFields.filter { f =>
    generator.isListOrSetType(f.fieldType)
  }).nonEmpty

  val construction_required_fields: Seq[StructFieldController] = fields.filter { f =>
    !f.field.constructionRequired
  }

  val has_construction_required_fields: Boolean = construction_required_fields.size > 0

  val has_map_fields: Boolean = cleanup(allFields.filter { f =>
    f.fieldType match {
      case MapType(_, _, _) => true
      case _ => false
    }
  }).nonEmpty

}
