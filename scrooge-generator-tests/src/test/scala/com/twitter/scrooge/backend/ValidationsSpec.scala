package com.twitter.scrooge.backend

import com.twitter.scrooge.backend.thriftscala.{
  NestedValidationStruct,
  ValidationException,
  ValidationStruct,
  ValidationUnion
}
import apache_java_thrift.{
  ValidationException => JValidationException,
  ValidationStruct => JValidationStruct,
  ValidationUnion => JValidationUnion,
  NestedValidationStruct => JNestedValidationStruct
}
import com.twitter.scrooge.testutil.JMockSpec
import com.twitter.scrooge.validation.ThriftValidationViolation
import scala.jdk.CollectionConverters._

class ValidationsSpec extends JMockSpec {
  "validateInstanceValue" should {
    "validate Struct" in { _ =>
      val validationStruct =
        ValidationStruct(
          "email",
          -1,
          101,
          0,
          0,
          Map("1" -> "1", "2" -> "2"),
          boolField = false,
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
      assertViolations(validationViolations, 8, violationMessages)
    }

    "validate nested Struct" in { _ =>
      val validationStruct =
        ValidationStruct(
          "email",
          -1,
          101,
          0,
          0,
          Map("1" -> "1", "2" -> "2"),
          boolField = false,
          "anything",
          Some("nothing"))
      val nestedValidationStruct = NestedValidationStruct(
        "not an email",
        validationStruct,
        Seq(validationStruct, validationStruct))
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
      assertViolations(validationViolations, 10, violationMessages)
    }

    "validate union" in { _ =>
      val validationIntUnion = ValidationUnion.IntField(-1)
      val validationStringUnion = ValidationUnion.StringField("")
      val validationIntViolations = ValidationUnion.validateInstanceValue(validationIntUnion)
      val validationStringViolations = ValidationUnion.validateInstanceValue(validationStringUnion)
      assertViolations(validationIntViolations, 1, Set("must be greater than or equal to 0"))
      assertViolations(validationStringViolations, 1, Set("must not be empty"))
    }

    "validate exception" in { _ =>
      val validationException = ValidationException("")
      val validationViolations = ValidationException.validateInstanceValue(validationException)
      assertViolations(validationViolations, 1, Set("must not be empty"))
    }
  }

  "Java validateInstanceValue" should {
    "validate Struct" in { _ =>
      val validationStruct =
        new JValidationStruct(
          "email",
          -1,
          101,
          0,
          0,
          Map("1" -> "1", "2" -> "2").asJava,
          false,
          "anything")
      val validationViolations = JValidationStruct.validateInstanceValue(validationStruct)
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

    "validate nested Struct" in { _ =>
      val validationStruct =
        new JValidationStruct(
          "email",
          -1,
          101,
          0,
          0,
          Map("1" -> "1", "2" -> "2").asJava,
          false,
          "anything")
      val nestedValidationStruct = new JNestedValidationStruct(
        "not an email",
        validationStruct,
        Seq(validationStruct, validationStruct).asJava)
      val validationViolations =
        JNestedValidationStruct.validateInstanceValue(nestedValidationStruct)
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

    "validate union" in { _ =>
      val validationIntUnion = new JValidationUnion()
      validationIntUnion.setIntField(-1)
      val validationStringUnion = new JValidationUnion()
      validationStringUnion.setStringField("")
      val validationIntViolations = JValidationUnion.validateInstanceValue(validationIntUnion)
      val validationStringViolations = JValidationUnion.validateInstanceValue(validationStringUnion)
      assertViolations(
        validationIntViolations.asScala.toSet,
        1,
        Set("must be greater than or equal to 0"))
      assertViolations(validationStringViolations.asScala.toSet, 1, Set("must not be empty"))
    }

    "validate exception" in { _ =>
      val validationException = new JValidationException("")
      val validationViolations = JValidationException.validateInstanceValue(validationException)
      assertViolations(validationViolations.asScala.toSet, 1, Set("must not be empty"))
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
