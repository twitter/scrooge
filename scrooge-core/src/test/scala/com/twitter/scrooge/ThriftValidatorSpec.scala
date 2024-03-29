package com.twitter.scrooge

import com.twitter.scrooge.thrift_validation.ThriftConstraintValidator
import com.twitter.scrooge.thrift_validation.ThriftValidationViolation
import com.twitter.scrooge.thrift_validation.ThriftValidator
import org.scalatest.Assertion
import org.scalatest.funsuite.AnyFunSuite

class ThriftValidatorSpec extends AnyFunSuite {
  val utilValidator: UtilValidator = UtilValidator()
  class CustomValidator extends ThriftValidator {
    override def customAnnotations = Map(
      "validation.invalid" -> InvalidConstraintValidator,
      "validation.emptyAnnotation" -> EmptyAnnotationValueConstraintValidator,
      "validation.charLengthInt" -> IntAnnotationValueConstraintValidator,
      "validation.byteEquals" -> ByteAnnotationValueConstraintValidator,
      "validation.shortEquals" -> ShortAnnotationValueConstraintValidator,
      "validation.longEquals" -> LongAnnotationValueConstraintValidator,
      "validation.successRate" -> DoubleAnnotationValueConstraintValidator
    )
  }
  val customThriftValidator = new CustomValidator

  test("validation.countryCode") {
    assertViolations(
      violations = utilValidator.validateField[String](
        "countryCode",
        "hello",
        Map("validation.countryCode" -> "")
      ),
      messages = Set("hello not a valid country code")
    )
  }

  test("validation.UUID") {
    assertViolations(
      violations = utilValidator.validateField[String](
        "UUID",
        "abcd",
        Map("validation.UUID" -> "")
      ),
      messages = Set("must be a valid UUID")
    )
  }

  test("validation.assertFalse") {
    assertViolations(
      violations = utilValidator.validateField[Boolean](
        "assertFalse",
        true,
        Map("validation.assertFalse" -> "")
      ),
      messages = Set("must be false")
    )
  }

  test("validation.assertTrue") {
    assertViolations(
      violations = utilValidator.validateField[Boolean](
        "assertFalse",
        false,
        Map("validation.assertTrue" -> "")
      ),
      messages = Set("must be true")
    )
  }

  test("validation.max") {
    assertViolations(
      violations = utilValidator.validateField[Long](
        "max",
        100L,
        Map("validation.max" -> "10")
      ),
      messages = Set("must be less than or equal to 10")
    )
    assertViolations(
      violations = utilValidator.validateField[Int](
        "max",
        100,
        Map("validation.max" -> "10")
      ),
      messages = Set("must be less than or equal to 10")
    )
    assertViolations(
      violations = utilValidator.validateField[Short](
        "max",
        100.toShort,
        Map("validation.max" -> "10")
      ),
      messages = Set("must be less than or equal to 10")
    )
    assertViolations(
      violations = utilValidator.validateField[Byte](
        "max",
        100.toByte,
        Map("validation.max" -> "10")
      ),
      messages = Set("must be less than or equal to 10")
    )
    assertViolations(
      violations = utilValidator.validateField[Double](
        "max",
        100.1,
        Map("validation.max" -> "10")
      ),
      messages = Set("must be less than or equal to 10")
    )
  }

  test("validation.min") {
    assertViolations(
      violations = utilValidator.validateField[Long](
        "min",
        0,
        Map("validation.min" -> "1")
      ),
      messages = Set("must be greater than or equal to 1")
    )
    assertViolations(
      violations = utilValidator.validateField[Int](
        "min",
        0,
        Map("validation.min" -> "1")
      ),
      messages = Set("must be greater than or equal to 1")
    )
    assertViolations(
      violations = utilValidator.validateField[Short](
        "min",
        0.toShort,
        Map("validation.min" -> "1")
      ),
      messages = Set("must be greater than or equal to 1")
    )
    assertViolations(
      violations = utilValidator.validateField[Byte](
        "min",
        0.toByte,
        Map("validation.min" -> "1")
      ),
      messages = Set("must be greater than or equal to 1")
    )
    assertViolations(
      violations = utilValidator.validateField[Double](
        "min",
        0.1,
        Map("validation.min" -> "1")
      ),
      messages = Set("must be greater than or equal to 1")
    )
  }

  test("validation.notEmpty") {
    assertViolations(
      violations = utilValidator.validateField[String](
        "notEmpty",
        "",
        Map("validation.notEmpty" -> "")
      ),
      messages = Set("must not be empty")
    )
    assertViolations(
      violations = utilValidator.validateField[Seq[Int]](
        "notEmpty",
        Seq.empty,
        Map("validation.notEmpty" -> "")
      ),
      messages = Set("must not be empty")
    )
    assertViolations(
      violations = utilValidator.validateField[Set[Int]](
        "notEmpty",
        Set.empty[Int],
        Map("validation.notEmpty" -> "")
      ),
      messages = Set("must not be empty")
    )
    assertViolations(
      violations = utilValidator.validateField[Map[Int, Int]](
        "notEmpty",
        Map.empty[Int, Int],
        Map("validation.notEmpty" -> "")
      ),
      messages = Set("must not be empty")
    )
  }

  test("validation.size") {
    // annotate on Seq field
    assertViolations(
      violations = utilValidator.validateField[Seq[Int]](
        "size",
        Seq(1, 2, 3),
        Map("validation.size.max" -> "2")
      ),
      // when "validation.size.min" is unspecified, the default is 0
      messages = Set("size must be between 0 and 2")
    )
    // annotate on Set field
    assertViolations(
      violations = utilValidator.validateField[Set[Double]](
        "size",
        Set(0.1),
        Map("validation.size.min" -> "2")
      ),
      // when "validation.size.max" is unspecified, the default is 2147483647
      messages = Set("size must be between 2 and 2147483647")
    )
    // annotate on Map field
    assertViolations(
      violations = utilValidator.validateField[Map[Int, String]](
        "size",
        Map(1 -> "1", 2 -> "2", 3 -> "3"),
        Map("validation.size.max" -> "2", "validation.size.min" -> "1")
      ),
      messages = Set("size must be between 1 and 2")
    )
  }

  test("validation.email") {
    assertViolations(
      violations = utilValidator.validateField[String](
        "email",
        "mails",
        Map("validation.email" -> "")
      ),
      messages = Set("must be a well-formed email address")
    )
  }

  test("validation.negative") {
    assertViolations(
      violations = utilValidator.validateField[Int](
        "negative",
        2,
        Map("validation.negative" -> "")
      ),
      messages = Set("must be less than 0")
    )
    assertViolations(
      violations = utilValidator.validateField[Long](
        "negative",
        2L,
        Map("validation.negative" -> "")
      ),
      messages = Set("must be less than 0")
    )
    assertViolations(
      violations = utilValidator.validateField[Double](
        "negative",
        2.0,
        Map("validation.negative" -> "")
      ),
      messages = Set("must be less than 0")
    )
    assertViolations(
      violations = utilValidator.validateField[Byte](
        "negative",
        2.toByte,
        Map("validation.negative" -> "")
      ),
      messages = Set("must be less than 0")
    )
    assertViolations(
      violations = utilValidator.validateField[Short](
        "negative",
        2.toShort,
        Map("validation.negative" -> "")
      ),
      messages = Set("must be less than 0")
    )
  }

  test("validation.negativeOrZero") {
    assertViolations(
      violations = utilValidator.validateField[Int](
        "negativeOrZero",
        2,
        Map("validation.negativeOrZero" -> "")
      ),
      messages = Set("must be less than or equal to 0")
    )
  }

  test("validation.positive") {
    assertViolations(
      violations = utilValidator.validateField[Int](
        "positive",
        -1,
        Map("validation.positive" -> "")
      ),
      messages = Set("must be greater than 0")
    )
  }

  test("validation.positiveOrZero") {
    assertViolations(
      violations = utilValidator.validateField[Int](
        "positiveOrZero",
        -1,
        Map("validation.positiveOrZero" -> "")
      ),
      messages = Set("must be greater than or equal to 0")
    )
  }

  test("validation.EAN") {
    assertViolations(
      violations = utilValidator.validateField[String](
        "EAN",
        "abc",
        Map("validation.EAN" -> "")
      ),
      messages = Set("invalid EAN13 barcode")
    )
  }

  test("validation.ISBN") {
    assertViolations(
      violations = utilValidator.validateField[String](
        "ISBN",
        "abc",
        Map("validation.ISBN" -> "")
      ),
      messages = Set("invalid ISBN")
    )
  }

  test("validation.length") {
    // only set min
    assertViolations(
      violations = utilValidator.validateField[String](
        "length",
        "123",
        Map("validation.length.min" -> "6")
      ),
      // when "validation.length.max" is unspecified, the default is 2147483647
      messages = Set("length must be between 6 and 2147483647")
    )
    // only set max
    assertViolations(
      violations = utilValidator.validateField[String](
        "length",
        "12345",
        Map("validation.length.max" -> "2")
      ),
      // when "validation.length.min" is unspecified, the default is 0
      messages = Set("length must be between 0 and 2")
    )
    // set both min and max
    assertViolations(
      violations = utilValidator.validateField[String](
        "length",
        "12345",
        Map("validation.length.min" -> "2", "validation.length.max" -> "4")
      ),
      messages = Set("length must be between 2 and 4")
    )
  }

  test("undefined annotation will skip validations") {
    val undefinedAnnotation = "validation.undefined"
    assert(!utilValidator.annotationIsDefined(undefinedAnnotation))
    assertViolations(
      violations = utilValidator.validateField(
        "undefined",
        "abc",
        Map(undefinedAnnotation -> "")
      ),
      messages = Set.empty
    )
  }

  test("build with custom annotations") {
    // The invalid validator will always through a runtime exception
    intercept[RuntimeException] {
      customThriftValidator.validateField(
        "custom",
        "abc",
        Map("validation.invalid" -> "")
      )
    }
    // The empty annotation validator will fail if the field doesn't start with "a"
    assertViolations(
      violations = customThriftValidator.validateField[String](
        "emptyAnnotation",
        "bcd",
        Map("validation.emptyAnnotation" -> "")
      ),
      messages = Set("The value bcd doesn't start with a")
    )
    // The Int annotation validator will fail if the field length is not 7
    assertViolations(
      violations = customThriftValidator.validateField[String](
        "intAnnotation",
        "1234",
        Map("validation.charLengthInt" -> "5")
      ),
      messages = Set("The length of the string 1234 is not 5.")
    )
    // The Byte annotation validator will fail is the field value isn't equal to the annotation value
    assertViolations(
      violations = customThriftValidator.validateField[Byte](
        "byteAnnotation",
        1.toByte,
        Map("validation.byteEquals" -> "2")
      ),
      messages = Set("1 does not equal to 2.")
    )
    // The Short annotation validator will fail is the field value isn't equal to the annotation value
    assertViolations(
      violations = customThriftValidator.validateField[Short](
        "shortAnnotation",
        1.toShort,
        Map("validation.shortEquals" -> "2")
      ),
      messages = Set("1 does not equal to 2.")
    )
    // The Long annotation validator will fail is the field value isn't equal to the annotation value
    assertViolations(
      violations = customThriftValidator.validateField[Long](
        "LongAnnotation",
        1L,
        Map("validation.longEquals" -> "2")
      ),
      messages = Set("1 does not equal to 2.")
    )
    // The Double annotation validator will fail is the field value < 99.97
    assertViolations(
      violations = customThriftValidator.validateField[Double](
        "doubleAnnotation",
        99.96,
        Map("validation.successRate" -> "99.97")
      ),
      messages = Set("99.96 is not greater than or equal to 99.97.")
    )
  }

  test("overriding existing annotations throws an IllegalArgumentException") {
    val existingValidation = "validation.min"

    intercept[IllegalArgumentException] {
      class ExistingValidator extends ThriftValidator {
        override def customAnnotations: Map[String, ThriftConstraintValidator[_, _]] =
          Map(existingValidation -> InvalidConstraintValidator)
      }
      new ExistingValidator
    }
  }

  test("a field with multiple annotations") {
    assertViolations(
      violations = utilValidator.validateField[Long](
        "multiple",
        0,
        Map("validation.min" -> "1", "validation.negative" -> "")
      ),
      messages = Set(
        "must be greater than or equal to 1",
        "must be less than 0"
      )
    )

    assertViolations(
      violations = utilValidator.validateField[String](
        "multiple",
        "",
        Map("validation.notEmpty" -> "", "validation.length.min" -> "2")
      ),
      messages = Set(
        "must not be empty",
        "length must be between 2 and 2147483647"
      )
    )

    assertViolations(
      violations = utilValidator.validateField[String](
        "multiple",
        "123",
        Map("validation.notEmpty" -> "", "validation.length.min" -> "5")
      ),
      messages = Set("length must be between 5 and 2147483647")
    )
  }

  private def assertViolations(
    violations: Set[ThriftValidationViolation],
    messages: Set[String]
  ): Set[Assertion] = {
    assert(violations.size == messages.size)
    violations.map(v => assert(messages.contains(v.violationMessage)))
  }

}
