package com.twitter.scrooge

import com.twitter.scrooge.validation.ThriftValidationViolation
import org.scalatest.funsuite.AnyFunSuite

class ThriftValidatorSpec extends AnyFunSuite {
  val thriftValidator: ThriftValidator = ThriftValidator()
  test("validation.countryCode") {
    assertViolations(
      violations = thriftValidator.validateField[String](
        "countryCode",
        "hello",
        Map("validation.countryCode" -> "")
      ),
      size = 1,
      messages = Set("hello not a valid country code")
    )
  }

  test("validation.UUID") {
    assertViolations(
      violations = thriftValidator.validateField[String](
        "UUID",
        "abcd",
        Map("validation.UUID" -> "")
      ),
      size = 1,
      messages = Set("must be a valid UUID")
    )
  }

  test("validation.assertFalse") {
    assertViolations(
      violations = thriftValidator.validateField[Boolean](
        "assertFalse",
        true,
        Map("validation.assertFalse" -> "")
      ),
      size = 1,
      messages = Set("must be false")
    )
  }

  test("validation.assertTrue") {
    assertViolations(
      violations = thriftValidator.validateField[Boolean](
        "assertFalse",
        false,
        Map("validation.assertTrue" -> "")
      ),
      size = 1,
      messages = Set("must be true")
    )
  }

  test("validation.max") {
    assertViolations(
      violations = thriftValidator.validateField[Long](
        "max",
        100L,
        Map("validation.max" -> "10")
      ),
      size = 1,
      messages = Set("must be less than or equal to 10")
    )
    assertViolations(
      violations = thriftValidator.validateField[Int](
        "max",
        100,
        Map("validation.max" -> "10")
      ),
      size = 1,
      messages = Set("must be less than or equal to 10")
    )
    assertViolations(
      violations = thriftValidator.validateField[Short](
        "max",
        100.toShort,
        Map("validation.max" -> "10")
      ),
      size = 1,
      messages = Set("must be less than or equal to 10")
    )
    assertViolations(
      violations = thriftValidator.validateField[Byte](
        "max",
        100.toByte,
        Map("validation.max" -> "10")
      ),
      size = 1,
      messages = Set("must be less than or equal to 10")
    )
    assertViolations(
      violations = thriftValidator.validateField[Double](
        "max",
        100.1,
        Map("validation.max" -> "10")
      ),
      size = 1,
      messages = Set("must be less than or equal to 10")
    )
  }

  test("validation.min") {
    assertViolations(
      violations = thriftValidator.validateField[Long](
        "min",
        0,
        Map("validation.min" -> "1")
      ),
      size = 1,
      messages = Set("must be greater than or equal to 1")
    )
    assertViolations(
      violations = thriftValidator.validateField[Int](
        "min",
        0,
        Map("validation.min" -> "1")
      ),
      size = 1,
      messages = Set("must be greater than or equal to 1")
    )
    assertViolations(
      violations = thriftValidator.validateField[Short](
        "min",
        0.toShort,
        Map("validation.min" -> "1")
      ),
      size = 1,
      messages = Set("must be greater than or equal to 1")
    )
    assertViolations(
      violations = thriftValidator.validateField[Byte](
        "min",
        0.toByte,
        Map("validation.min" -> "1")
      ),
      size = 1,
      messages = Set("must be greater than or equal to 1")
    )
    assertViolations(
      violations = thriftValidator.validateField[Double](
        "min",
        0.1,
        Map("validation.min" -> "1")
      ),
      size = 1,
      messages = Set("must be greater than or equal to 1")
    )
  }

  test("validation.notEmpty") {
    assertViolations(
      violations = thriftValidator.validateField(
        "notEmpty",
        "",
        Map("validation.notEmpty" -> "")
      ),
      size = 1,
      messages = Set("must not be empty")
    )
  }

  test("validation.size") {
    // annotate on Seq field
    assertViolations(
      violations = thriftValidator.validateField[Seq[Int]](
        "size",
        Seq(1, 2, 3),
        Map("validation.size.max" -> "2")
      ),
      size = 1,
      // when "validation.size.min" is unspecified, the default is 0
      messages = Set("size must be between 0 and 2")
    )
    // annotate on Set field
    assertViolations(
      violations = thriftValidator.validateField[Set[Double]](
        "size",
        Set(0.1),
        Map("validation.size.min" -> "2")
      ),
      size = 1,
      // when "validation.size.max" is unspecified, the default is 2147483647
      messages = Set("size must be between 2 and 2147483647")
    )
    // annotate on Map field
    assertViolations(
      violations = thriftValidator.validateField[Map[Int, String]](
        "size",
        Map(1 -> "1", 2 -> "2", 3 -> "3"),
        Map("validation.size.max" -> "2", "validation.size.min" -> "1")
      ),
      size = 1,
      messages = Set("size must be between 1 and 2")
    )
  }

  test("validation.email") {
    assertViolations(
      violations = thriftValidator.validateField[String](
        "email",
        "mails",
        Map("validation.email" -> "")
      ),
      size = 1,
      messages = Set("must be a well-formed email address")
    )
  }

  test("validation.negative") {
    assertViolations(
      violations = thriftValidator.validateField[Int](
        "negative",
        2,
        Map("validation.negative" -> "")
      ),
      size = 1,
      messages = Set("must be less than 0")
    )
    assertViolations(
      violations = thriftValidator.validateField[Long](
        "negative",
        2L,
        Map("validation.negative" -> "")
      ),
      size = 1,
      messages = Set("must be less than 0")
    )
    assertViolations(
      violations = thriftValidator.validateField[Double](
        "negative",
        2.0,
        Map("validation.negative" -> "")
      ),
      size = 1,
      messages = Set("must be less than 0")
    )
  }

  test("validation.negativeOrZero") {
    assertViolations(
      violations = thriftValidator.validateField[Int](
        "negativeOrZero",
        2,
        Map("validation.negativeOrZero" -> "")
      ),
      size = 1,
      messages = Set("must be less than or equal to 0")
    )
  }

  test("validation.positive") {
    assertViolations(
      violations = thriftValidator.validateField[Int](
        "positive",
        -1,
        Map("validation.positive" -> "")
      ),
      size = 1,
      messages = Set("must be greater than 0")
    )
  }

  test("validation.positiveOrZero") {
    assertViolations(
      violations = thriftValidator.validateField[Int](
        "positiveOrZero",
        -1,
        Map("validation.positiveOrZero" -> "")
      ),
      size = 1,
      messages = Set("must be greater than or equal to 0")
    )
  }

  test("validation.EAN") {
    assertViolations(
      violations = thriftValidator.validateField(
        "EAN",
        "abc",
        Map("validation.EAN" -> "")
      ),
      size = 1,
      messages = Set("invalid EAN13 barcode")
    )
  }

  test("validation.ISBN") {
    assertViolations(
      violations = thriftValidator.validateField(
        "ISBN",
        "abc",
        Map("validation.ISBN" -> "")
      ),
      size = 1,
      messages = Set("invalid ISBN")
    )
  }

  test("validation.length") {
    // only set min
    assertViolations(
      violations = thriftValidator.validateField[String](
        "length",
        "123",
        Map("validation.length.min" -> "6")
      ),
      size = 1,
      // when "validation.length.max" is unspecified, the default is 2147483647
      messages = Set("length must be between 6 and 2147483647")
    )
    // only set max
    assertViolations(
      violations = thriftValidator.validateField[String](
        "length",
        "12345",
        Map("validation.length.max" -> "2")
      ),
      size = 1,
      // when "validation.length.min" is unspecified, the default is 0
      messages = Set("length must be between 0 and 2")
    )
    // set both min and max
    assertViolations(
      violations = thriftValidator.validateField[String](
        "length",
        "12345",
        Map("validation.length.min" -> "2", "validation.length.max" -> "4")
      ),
      size = 1,
      messages = Set("length must be between 2 and 4")
    )
  }

  test("undefined annotation will skip validations") {
    val thriftValidator = ThriftValidator()
    val undefinedAnnotation = "validation.undefined"
    assert(!thriftValidator.annotationIsDefined(undefinedAnnotation))
    assertViolations(
      violations = thriftValidator.validateField(
        "undefined",
        "abc",
        Map(undefinedAnnotation -> "")
      ),
      size = 0,
      messages = Set.empty
    )
  }

  test("build with custom annotations") {
    val thriftValidator = ThriftValidator
      .newBuilder()
      .withConstraints(
        Map(
          "validation.invalid" -> InvalidConstraintValidator,
          "validation.emptyAnnotation" -> EmptyAnnotationValueConstraintValidator,
          "validation.charLengthInt" -> IntAnnotationValueConstraintValidator,
          "validation.byteEquals" -> ByteAnnotationValueConstraintValidator,
          "validation.shortEquals" -> ShortAnnotationValueConstraintValidator,
          "validation.longEquals" -> LongAnnotationValueConstraintValidator,
          "validation.successRate" -> DoubleAnnotationValueConstraintValidator
        )
      ).build()

    // The invalid validator will always through a runtime exception
    intercept[RuntimeException] {
      thriftValidator.validateField(
        "custom",
        "abc",
        Map("validation.invalid" -> "")
      )
    }
    // The empty annotation validator will fail if the field doesn't start with "a"
    assertViolations(
      violations = thriftValidator.validateField[String](
        "emptyAnnotation",
        "bcd",
        Map("validation.emptyAnnotation" -> "")
      ),
      size = 1,
      messages = Set("The value bcd doesn't start with a")
    )
    // The Int annotation validator will fail if the field length is not 7
    assertViolations(
      violations = thriftValidator.validateField[String](
        "intAnnotation",
        "1234",
        Map("validation.charLengthInt" -> "5")
      ),
      size = 1,
      messages = Set("The length of the string 1234 is not 5.")
    )
    // The Byte annotation validator will fail is the field value isn't equal to the annotation value
    assertViolations(
      violations = thriftValidator.validateField[Byte](
        "byteAnnotation",
        1.toByte,
        Map("validation.byteEquals" -> "2")
      ),
      size = 1,
      messages = Set("1 does not equal to 2.")
    )
    // The Short annotation validator will fail is the field value isn't equal to the annotation value
    assertViolations(
      violations = thriftValidator.validateField[Short](
        "shortAnnotation",
        1.toShort,
        Map("validation.shortEquals" -> "2")
      ),
      size = 1,
      messages = Set("1 does not equal to 2.")
    )
    // The Long annotation validator will fail is the field value isn't equal to the annotation value
    assertViolations(
      violations = thriftValidator.validateField[Long](
        "LongAnnotation",
        1L,
        Map("validation.longEquals" -> "2")
      ),
      size = 1,
      messages = Set("1 does not equal to 2.")
    )
    // The Double annotation validator will fail is the field value < 99.97
    assertViolations(
      violations = thriftValidator.validateField[Double](
        "doubleAnnotation",
        99.96,
        Map("validation.successRate" -> "99.97")
      ),
      size = 1,
      messages = Set("99.96 is not greater than or equal to 99.97.")
    )
  }

  test("overriding existing annotations throws an IllegalArgumentException") {
    val existingValidation = "validation.min"

    intercept[IllegalArgumentException] {
      ThriftValidator
        .newBuilder()
        .withConstraints(
          Map(existingValidation -> InvalidConstraintValidator)
        ).build()
    }
  }

  test("a field with multiple annotations") {
    assertViolations(
      violations = thriftValidator.validateField[Long](
        "multiple",
        0,
        Map("validation.min" -> "1", "validation.negative" -> "")
      ),
      size = 2,
      messages = Set(
        "must be greater than or equal to 1",
        "must be less than 0"
      )
    )

    assertViolations(
      violations = thriftValidator.validateField[String](
        "multiple",
        "",
        Map("validation.notEmpty" -> "", "validation.length.min" -> "2")
      ),
      size = 2,
      messages = Set(
        "must not be empty",
        "length must be between 2 and 2147483647"
      )
    )

    assertViolations(
      violations = thriftValidator.validateField[String](
        "multiple",
        "123",
        Map("validation.notEmpty" -> "", "validation.length.min" -> "5")
      ),
      size = 1,
      messages = Set("length must be between 5 and 2147483647")
    )
  }

  private def assertViolations(
    violations: Set[ThriftValidationViolation],
    size: Int,
    messages: Set[String]
  ) = {
    assert(violations.size == size)
    violations.map(v => assert(messages.contains(v.violationMessage)))
  }

}
