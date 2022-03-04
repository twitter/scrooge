package com.twitter.scrooge.java_generator

import apache_java_thrift._
import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.finagle.Address
import com.twitter.finagle.Name
import com.twitter.finagle.Thrift
import com.twitter.scrooge.testutil.Spec
import com.twitter.scrooge.thrift_validation.ThriftValidationViolation
import com.twitter.util.Await
import com.twitter.util.Awaitable
import com.twitter.util.Duration
import com.twitter.util.Future
import org.apache.thrift.TApplicationException
import java.lang
import java.util
import java.net.InetSocketAddress
import scala.jdk.CollectionConverters._

class ValidationsJavaGeneratorSpec extends Spec {
  private[this] def await[T](a: Awaitable[T], d: Duration = 5.seconds): T =
    Await.result(a, d)

  private[this] class ValidationServiceImpl extends ValidationService.ServiceIface {
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
      val server = Thrift.server.serveIface("localhost:*", impl)
      val client = Thrift.client.build[ValidationService.ServiceIface](
        Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
        "a_client")
      intercept[TApplicationException] {
        await(client.validate(validationStruct, validationIntUnion, validationException))
      }
    }

    "violationReturning API should return violations instead of throwing exceptions for invalid requests" in {
      class ViolationReturningService extends ValidationService.ServerValidationMixin {
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

        override def violationReturningValidate(
          structRequest: ValidationStruct,
          unionRequest: ValidationUnion,
          exceptionRequest: ValidationException,
          structRequestViolations: util.Set[ThriftValidationViolation],
          unionRequestViolations: util.Set[ThriftValidationViolation],
          exceptionRequestViolations: util.Set[ThriftValidationViolation]
        ): Future[lang.Boolean] = {
          // should return false if `structRequest` is invalid
          Future.value(structRequestViolations.isEmpty)
        }

        override def violationReturningValidateOption(
          structRequest: ValidationStruct,
          unionRequest: ValidationUnion,
          exceptionRequest: ValidationException,
          structRequestViolations: util.Set[ThriftValidationViolation],
          unionRequestViolations: util.Set[ThriftValidationViolation],
          exceptionRequestViolations: util.Set[ThriftValidationViolation]
        ): Future[lang.Boolean] = {
          // should return false if `structRequest` is invalid
          Future.value(structRequestViolations.isEmpty)
        }
      }
      val validationStruct = new ValidationStruct(
        "email",
        -1,
        101,
        0,
        0,
        Map("1" -> "1", "2" -> "2").asJava,
        false,
        "anything")
      val validationIntUnion = new ValidationUnion()
      validationIntUnion.setUnionIntField(-1)
      val validationException = new ValidationException("")

      val iface = new ViolationReturningService
      val server = Thrift.server.serveIface("localhost:*", iface)
      val client = Thrift.client.build[ValidationService.ServiceIface](
        Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
        "a_client")

      // returns false, without throwing an exception, since it redirects to the violationReturning version
      assert(!await(client.validate(validationStruct, validationIntUnion, validationException)))
      assert(
        !await(client.validateOption(validationStruct, validationIntUnion, validationException)))
      // throw an exception since there is no violationReturning implementation for the API `validateOnlyValidatedRequest`
      // it preserves the default behavior to throw an exception for invalid request
      intercept[TApplicationException] {
        await(client.validateOnlyValidatedRequest(validationStruct))
      }
    }

    "validate null request" in {
      val impl = new ValidationServiceImpl()
      val server = Thrift.server.serveIface("localhost:*", impl)
      val client = Thrift.client.build[ValidationService.ServiceIface](
        Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
        "a_client")
      //null values passed after code generation aren't checked so
      // we catch NullPointerException in the mustache file
      await(client.validate(null, null, null))
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
