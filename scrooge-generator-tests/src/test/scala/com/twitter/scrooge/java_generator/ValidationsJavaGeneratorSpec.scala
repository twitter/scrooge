package com.twitter.scrooge.java_generator

import apache_java_thrift._
import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.finagle.Address
import com.twitter.finagle.Name
import com.twitter.finagle.ThriftMux
import com.twitter.scrooge.testutil.Spec
import com.twitter.scrooge.thrift_validation.ThriftValidationViolation
import com.twitter.util.Await
import com.twitter.util.Awaitable
import com.twitter.util.Duration
import com.twitter.util.Future
import java.lang
import java.net.InetSocketAddress
import org.apache.thrift.TApplicationException
import scala.jdk.CollectionConverters._

class ValidationsJavaGeneratorSpec extends Spec {
  def await[T](a: Awaitable[T], d: Duration = 5.seconds): T =
    Await.result(a, d)

  private class ValidationServiceImpl extends ValidationService.ServiceIface {
    override def validate(
      structRequest: ValidationStruct,
      unionRequest: ValidationUnion,
      exceptionRequest: ValidationException
    ): Future[lang.Boolean] = Future.value(true)

    override def validateOption(
      structRequest: ValidationStruct,
      unionRequest: ValidationUnion,
      exceptionRequest: ValidationException
    ): Future[lang.Boolean] = Future.value(true)

    override def validateWithNonValidatedRequest(
      validationRequest: ValidationStruct,
      nonValidationRequest: NoValidationStruct
    ): Future[lang.Boolean] = Future.value(true)

    override def validateOnlyNonValidatedRequest(
      nonValidationRequest: NoValidationStruct
    ): Future[lang.Boolean] = Future.value(true)

    override def validateOnlyValidatedRequest(
      validationRequest: ValidationStruct
    ): Future[lang.Boolean] = Future.value(true)
  }

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
      validationIntUnion.setUnionIntField(-1)
      val validationStringUnion = new ValidationUnion()
      validationStringUnion.setUnionStringField("")
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

    "validate struct, union and exception request" in {
      val validationStruct = new ValidationStruct(
        "email",
        -1,
        101,
        0,
        0,
        Map("1" -> "1", "2" -> "2").asJava,
        false,
        "anything")
      val impl = new ValidationServiceImpl()
      val validationIntUnion = new ValidationUnion()
      validationIntUnion.setUnionIntField(-1)
      val validationException = new ValidationException("")
      val muxServer = ThriftMux.server.serveIface("localhost:*", impl)
      val muxClient = ThriftMux.client.build[ValidationService.ServiceIface](
        Name.bound(Address(muxServer.boundAddress.asInstanceOf[InetSocketAddress])),
        "a_client")
      intercept[TApplicationException] {
        await(muxClient.validate(validationStruct, validationIntUnion, validationException))
      }
    }

    "validate null request" in {
      val impl = new ValidationServiceImpl()
      val muxServer = ThriftMux.server.serveIface("localhost:*", impl)
      val muxClient = ThriftMux.client.build[ValidationService.ServiceIface](
        Name.bound(Address(muxServer.boundAddress.asInstanceOf[InetSocketAddress])),
        "a_client")
      //null values passed after code generation aren't checked so
      // we catch NullPointerException in the mustache file
      await(muxClient.validate(null, null, null))
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
