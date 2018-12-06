package com.twitter.scrooge.android_generator

import com.twitter.scrooge.ast._
import com.twitter.scrooge.ast.Field
import com.twitter.scrooge.java_generator.{TypeController => JavaTypeController}

class StructController(
  struct: StructLike,
  val in_class: Boolean,
  generator: AndroidGenerator,
  ns: Option[Identifier],
  val is_result: Boolean = false)
    extends JavaTypeController(struct, generator, ns) {
  val struct_type_name = generator.typeName(StructType(struct))
  val is_final = false // TODO: not sure if we need this annotations support
  val is_exception = struct.isInstanceOf[Exception_]
  val is_union = struct.isInstanceOf[Union]
  val allFields = struct.fields

  def cleanup(fields: Seq[Field]): Seq[StructFieldController] = {
    fields.zipWithIndex map {
      case (f, i) =>
        val serializePrefix = if (is_union) "" else "this."
        new StructFieldController(f, i, fields.size, generator, ns, serializePrefix)
    }
  }
  val fields = cleanup(allFields)
  val has_fields = fields.size > 0
  val sorted_fields = cleanup(allFields sortBy { f =>
    f.index
  })

  val non_nullable_fields = cleanup(allFields.filter { f =>
    !generator.isNullableType(f.fieldType)
  })

  val has_non_nullable_fields = non_nullable_fields.size > 0

  val has_bit_vector = non_nullable_fields.size > 0

  val default_fields = cleanup(allFields.filter { f =>
    !f.default.isEmpty
  })

  val non_optional_fields = cleanup(allFields.filter { f =>
    !f.requiredness.isOptional
  })
  val non_default_constructor = non_optional_fields.size > 0

  val has_list_or_set_fields = cleanup(allFields.filter { f =>
    generator.isListOrSetType(f.fieldType)
  }).nonEmpty

  val has_map_fields = cleanup(allFields.filter { f =>
    f.fieldType match {
      case MapType(_, _, _) => true
      case _ => false
    }
  }).nonEmpty

}
