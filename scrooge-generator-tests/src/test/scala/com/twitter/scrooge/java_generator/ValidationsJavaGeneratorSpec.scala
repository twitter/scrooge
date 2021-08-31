package com.twitter.scrooge.java_generator

import apache_java_thrift._
import com.twitter.scrooge.testutil.Spec
import com.twitter.scrooge.validation.ThriftValidationViolation
import scala.jdk.CollectionConverters._

class ValidationsJavaGeneratorSpec extends Spec {
  "Java validateInstanceValue" should {
    "validate Struct" in {
      val validationStruct =
        new ValidationStruct(
          "email",
          -1,
          101,
          0,
          0,
          Map("1" -> "1", "2" -> "2").asJava,
          false,
          "anything")
      val validationViolations = ValidationStruct.validateInstanceValue(validationStruct)
      val violationMessages = Set(
        "length must be between 6 and 2147483647",
        "must be a well-formed email address",
        "must be true",
        "size must be between 0 and 1",
        "must be less than 0",
        "must be less than or equal to 100",
        "must be greater than 0",
        "must be greater than or equal to 0"
      )
      assertViolations(validationViolations.asScala.toSet, 8, violationMessages)
    }

    "validate nested Struct" in {
      val validationStruct =
        new ValidationStruct(
          "email",
          -1,
          101,
          0,
          0,
          Map("1" -> "1", "2" -> "2").asJava,
          false,
          "anything")
      val nestedValidationStruct = new NestedValidationStruct(
        "not an email",
        validationStruct,
        Seq(validationStruct, validationStruct).asJava)
      val validationViolations =
        NestedValidationStruct.validateInstanceValue(nestedValidationStruct)
      val violationMessages = Set(
        "length must be between 6 and 2147483647",
        "must be a well-formed email address",
        "must be true",
        "size must be between 0 and 1",
        "must be less than 0",
        "must be less than or equal to 100",
        "must be greater than 0",
        "must be greater than or equal to 0"
      )
      assertViolations(validationViolations.asScala.toSet, 10, violationMessages)
    }

    "validate union" in {
      val validationIntUnion = new ValidationUnion()
      validationIntUnion.setIntField(-1)
      val validationStringUnion = new ValidationUnion()
      validationStringUnion.setStringField("")
      val validationIntViolations = ValidationUnion.validateInstanceValue(validationIntUnion)
      val validationStringViolations = ValidationUnion.validateInstanceValue(validationStringUnion)
      assertViolations(
        validationIntViolations.asScala.toSet,
        1,
        Set("must be greater than or equal to 0"))
      assertViolations(validationStringViolations.asScala.toSet, 1, Set("must not be empty"))
    }

    "validate exception" in {
      val validationException = new ValidationException("")
      val validationViolations = ValidationException.validateInstanceValue(validationException)
      assertViolations(validationViolations.asScala.toSet, 1, Set("must not be empty"))
    }

    "skip annotations not for ThriftValidator" in {
      val nonValidationStruct = new NonValidationStruct("anything")
      val validationViolations = NonValidationStruct.validateInstanceValue(nonValidationStruct)
      assertViolations(validationViolations.asScala.toSet, 0, Set.empty)
    }
  }

  "Java custom validator" should {
    "validate Struct" in {
      val validationStruct =
        new JCustomValidationStruct(
          "email",
          "abc",
          101,
          0,
          0,
          Map("1" -> "1", "2" -> "2").asJava,
          false)
      val validationViolations = JCustomValidationStruct.validateInstanceValue(validationStruct)
      val violationMessages = Set(
        "length must be between 6 and 2147483647",
        "must be a well-formed email address",
        "must be true",
        "size must be between 0 and 1",
        "must be less than 0",
        "must be greater than 0",
        "must start with a",
        "invalid user id",
        "invalid user screen name"
      )
      assertViolations(validationViolations.asScala.toSet, 9, violationMessages)
    }

    "validate nested Struct" in {
      val validationStruct =
        new JCustomValidationStruct(
          "email",
          "abc",
          101,
          0,
          0,
          Map("1" -> "1", "2" -> "2").asJava,
          false)
      val nestedValidationStruct = new JCustomNestedValidationStruct(
        "not an email",
        validationStruct,
        Seq(validationStruct, validationStruct).asJava)
      val validationViolations =
        JCustomNestedValidationStruct.validateInstanceValue(nestedValidationStruct)
      val violationMessages = Set(
        "length must be between 6 and 2147483647",
        "must be a well-formed email address",
        "must be true",
        "size must be between 0 and 1",
        "must be less than 0",
        "must be less than or equal to 100",
        "must be greater than 0",
        "must be greater than or equal to 0",
        "must start with a",
        "invalid user id",
        "invalid user screen name"
      )
      assertViolations(validationViolations.asScala.toSet, 12, violationMessages)
    }

    "validate union" in {
      val validationIntUnion = new JCustomValidationUnion()
      validationIntUnion.setUserId(1)
      val validationStringUnion = new JCustomValidationUnion()
      validationStringUnion.setScreenName("")
      val validationIntViolations = JCustomValidationUnion.validateInstanceValue(validationIntUnion)
      val validationStringViolations =
        JCustomValidationUnion.validateInstanceValue(validationStringUnion)

      assertViolations(validationIntViolations.asScala.toSet, 1, Set("invalid user id"))
      assertViolations(
        validationStringViolations.asScala.toSet,
        2,
        Set("must not be empty", "invalid user screen name"))
    }

    "validate exception" in {
      val validationException = new JCustomValidationException("")
      val validationViolations =
        JCustomValidationException.validateInstanceValue(validationException)
      assertViolations(
        validationViolations.asScala.toSet,
        2,
        Set("must not be empty", "must start with a"))
    }
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
