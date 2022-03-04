package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast.Exception_
import com.twitter.scrooge.ast.Field
import com.twitter.scrooge.ast.Identifier
import com.twitter.scrooge.ast.Requiredness
import com.twitter.scrooge.ast.Struct
import com.twitter.scrooge.ast.StructType
import com.twitter.scrooge.ast.Union
import com.twitter.scrooge.backend.Generator
import com.google.common.base

object FieldController {
  // return true if the `Field` is of struct type (struct, union, or exception), and any fields
  // of the `Field` has validation annotation (annotation key starts with "validation.")
  def hasValidationAnnotation(field: Field): Boolean =
    field.fieldType match {
      case structType: StructType =>
        val structLike = structType.struct
        structLike match {
          case _: Struct | _: Union | _: Exception_ =>
            structLike.fields.exists(_.hasValidationAnnotation)
          case _ => false
        }
      case _ => false
    }
}

class FieldController(f: Field, generator: ApacheJavaGenerator, ns: Option[Identifier])
    extends BaseController(generator, ns) {
  val field_name: String = f.sid.name
  val requirement: String = getRequirement(f)
  val default: Boolean = !f.default.isEmpty
  val optional: Boolean = f.requiredness.isOptional
  val required: Boolean = f.requiredness.isRequired
  val constructionRequired: Boolean = Generator.isConstructionRequiredField(f)
  val has_annotations: Boolean = f.fieldAnnotations.nonEmpty

  val field_type: FieldTypeController = new FieldTypeController(f.fieldType, generator)
  val field_arg: String = "args." + f.sid.name
  val arg_type: String = generator.typeName(f.fieldType)

  def getRequirement(field: Field): String = {
    field.requiredness match {
      case Requiredness.Required => "TFieldRequirementType.REQUIRED"
      case Requiredness.Optional => "TFieldRequirementType.OPTIONAL"
      case _ => "TFieldRequirementType.DEFAULT"
    }
  }

  val i_if_nullable: base.Function[String, String] = newHelper { input =>
    if (generator.isNullableType(f.fieldType)) indent(input, 2, false) else input
  }

  val i_if_optional: base.Function[String, String] = newHelper { input =>
    if (optional) indent(input, 2, false) else input
  }

  // thrift validations utilities
  val violationArg: String = f.sid.append("Violations").name
}
