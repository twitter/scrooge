package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast.{Field, _}
import com.twitter.scrooge.backend.Generator
import java.util.{Map => JMap, Set => JSet}
import scala.collection.JavaConverters._

class StructController(
  struct: StructLike,
  val in_class: Boolean,
  generator: ApacheJavaGenerator,
  ns: Option[Identifier],
  val is_result: Boolean = false
) extends TypeController(struct, generator, ns) {

  val struct_type_name: String = generator.typeName(StructType(struct))

  val is_final: Boolean = false // TODO: not sure if we need this annotations support

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

  val sorted_fields: Seq[StructFieldController] = cleanup(allFields sortBy { f =>
    f.index
  })

  val non_nullable_fields: Seq[StructFieldController] = cleanup(allFields.filter { f =>
    !generator.isNullableType(f.fieldType)
  })

  val has_non_nullable_fields: Boolean = non_nullable_fields.size > 0

  val has_bit_vector: Boolean = non_nullable_fields.size > 0

  val default_fields: Seq[StructFieldController] = cleanup(allFields.filter { f =>
    !f.default.isEmpty
  })

  val non_optional_fields: Seq[StructFieldController] = cleanup(allFields.filter { f =>
    !f.requiredness.isOptional || Generator.isConstructionRequiredField(f)
  })
  val non_default_constructor: Boolean = non_optional_fields.size > 0

  val struct_annotations: JSet[JMap.Entry[String, String]] = struct.annotations.asJava.entrySet()

  val has_struct_annotations: Boolean = !struct_annotations.isEmpty

  val field_annotations: JSet[JMap.Entry[Field, JSet[JMap.Entry[String, String]]]] = allFields
    .filter(_.fieldAnnotations.nonEmpty).map { field =>
      field -> field.fieldAnnotations.asJava.entrySet()
    }.toMap.asJava.entrySet()

  val has_field_annotations: Boolean = !field_annotations.isEmpty

  val has_default_value: Seq[Field] = allFields.filter(_.default.nonEmpty)
}
