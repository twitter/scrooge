package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast.Field
import com.twitter.scrooge.ast.Identifier
import com.twitter.scrooge.ast.Requiredness
import com.twitter.scrooge.backend.Generator
import com.google.common.base

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
}
