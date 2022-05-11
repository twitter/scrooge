package com.twitter.scrooge.backend

import org.scalatest.funsuite.AnyFunSuite

class AnnotationValidatorTest extends AnyFunSuite {
  test("validate default annotation applies to the wrong field") {
    val defaultWrongField =
      AnnotationValidator.validateAnnotations(Set(classOf[String]), Map("validation.min" -> "2"))
    val defaultWrongFieldMessage = Set(
      "The annotation validation.min can not be applied to the field with type String"
    )
    assertAnnotationViolations(defaultWrongField, defaultWrongFieldMessage)
    val defaultWrongCollection =
      AnnotationValidator.validateAnnotations(
        Set(classOf[Map[Int, String]]),
        Map("validation.min" -> "2"))
    val defaultWrongCollectionMessage = Set(
      "The annotation validation.min can not be applied to the field with type Map"
    )
    assertAnnotationViolations(defaultWrongCollection, defaultWrongCollectionMessage)
  }

  test("validate default annotation with incompatible value") {
    val defaultIncompatible =
      AnnotationValidator.validateAnnotations(Set(classOf[Int]), Map("validation.min" -> "hello"))
    val defaultIncompatibleMessage = Set(
      "The annotation validation.min requires a value of type Long, the annotation value hello is not of type Long"
    )
    assertAnnotationViolations(defaultIncompatible, defaultIncompatibleMessage)
    val defaultIncompatibleLength =
      AnnotationValidator.validateAnnotations(
        Set(classOf[String]),
        Map("validation.length.min" -> "hello"))
    val defaultIncompatibleMessageLength = Set(
      "The annotation validation.length.min requires a value of type Integer, the annotation value hello is not of type Integer"
    )
    assertAnnotationViolations(defaultIncompatibleLength, defaultIncompatibleMessageLength)
  }

  test("validate default annotation with incompatible value applies to the wrong field") {
    val defaultWrongFieldIncompatible =
      AnnotationValidator.validateAnnotations(
        Set(classOf[String]),
        Map("validation.min" -> "hello"))
    val defaultWrongFieldIncompatibleMessage = Set(
      "The annotation validation.min requires a value of type Long, the annotation value hello is not of type Long",
      "The annotation validation.min can not be applied to the field with type String"
    )
    assertAnnotationViolations(defaultWrongFieldIncompatible, defaultWrongFieldIncompatibleMessage)
  }

  test("validate multiple wrong default annotations") {
    // including annotation applying to the wrong field and annotation with incompatible value
    val multiDefaultViolations = AnnotationValidator.validateAnnotations(
      Set(classOf[Set[String]]),
      Map("validation.min" -> "hello", "validation.assertTrue" -> ""))
    val multiDefaultViolationsMessage = Set(
      "The annotation validation.min can not be applied to the field with type Set",
      "The annotation validation.min requires a value of type Long, the annotation value hello is not of type Long",
      "The annotation validation.assertTrue can not be applied to the field with type Set"
    )
    assertAnnotationViolations(multiDefaultViolations, multiDefaultViolationsMessage)
  }

  private def assertAnnotationViolations(
    violations: Iterable[String],
    messages: Set[String]
  ) = {
    assert(violations.size == messages.size)
    messages.map { v =>
      assert(violations.exists(_.contains(v)))
    }
  }
}
