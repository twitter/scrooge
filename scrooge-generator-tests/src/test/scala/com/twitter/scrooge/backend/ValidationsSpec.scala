package com.twitter.scrooge.backend

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.finagle.Address
import com.twitter.finagle.Name
import com.twitter.finagle.Thrift
import com.twitter.scrooge.Request
import com.twitter.scrooge.backend.thriftscala.ValidationService
import com.twitter.scrooge.backend.thriftscala._
import com.twitter.scrooge.testutil.JMockSpec
import com.twitter.scrooge.thrift_validation.ThriftValidationViolation
import com.twitter.util.Await
import com.twitter.util.Awaitable
import com.twitter.util.Duration
import com.twitter.util.Future
import java.net.InetAddress
import java.net.InetSocketAddress
import org.apache.thrift.TApplicationException

class ValidationsSpec extends JMockSpec {

  def await[T](a: Awaitable[T], d: Duration = 100.seconds): T = Await.result(a, d)

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

  val noValidationStruct =
    NoValidationStruct(
      "email",
      -1,
      101,
      0,
      0,
      Map("1" -> "1", "2" -> "2"),
      boolField = false,
      "anything",
      Some("nothing"))

  val validationException = ValidationException("")

  val validationUnion = ValidationUnion.UnionIntField(-1)

  val methodPerEndpoint = new ValidationService.MethodPerEndpoint with ServerValidationMixin {
    override def validate(
      structRequest: ValidationStruct,
      unionRequest: ValidationUnion,
      exceptionRequest: ValidationException
    ): Future[Boolean] = Future.True

    override def validateOption(
      structRequest: Option[ValidationStruct],
      unionRequest: Option[ValidationUnion],
      exceptionRequest: Option[ValidationException]
    ): Future[Boolean] = Future.True

    override def validateOnlyValidatedRequest(
      validationRequest: ValidationStruct
    ): Future[Boolean] = Future.True

    override def validateWithNonValidatedRequest(
      validationRequest: ValidationStruct,
      nonValidationRequest: NoValidationStruct
    ): Future[Boolean] = Future.True

    override def validateOnlyNonValidatedRequest(
      nonValidationRequest: NoValidationStruct
    ): Future[Boolean] = Future.True
  }

  val thriftServer =
    Thrift.server
      .serveIface(new InetSocketAddress(InetAddress.getLoopbackAddress, 0), methodPerEndpoint)

  val methodPerEndpointClient = Thrift.client.build[ValidationService.MethodPerEndpoint](
    Name.bound(Address(thriftServer.boundAddress.asInstanceOf[InetSocketAddress])),
    "client"
  )

  "validateInstanceValue" should {
    "validate Struct" in { _ =>
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
      val validationIntUnion = ValidationUnion.UnionIntField(-1)
      val validationStringUnion = ValidationUnion.UnionStringField("")
      val validationIntViolations = ValidationUnion.validateInstanceValue(validationIntUnion)
      val validationStringViolations = ValidationUnion.validateInstanceValue(validationStringUnion)
      assertViolations(validationIntViolations, 1, Set("must be greater than or equal to 0"))
      assertViolations(validationStringViolations, 1, Set("must not be empty"))
    }

    "validate exception" in { _ =>
      val validationViolations = ValidationException.validateInstanceValue(validationException)
      assertViolations(validationViolations, 1, Set("must not be empty"))
    }

    "skip annotations not for ThriftValidator" in { _ =>
      val nonValidationStruct = NonValidationStruct("anything")
      val validationViolations = NonValidationStruct.validateInstanceValue(nonValidationStruct)
      assertViolations(validationViolations, 0, Set.empty)
    }

    "validate struct, union and exception request" in { _ =>
      intercept[TApplicationException] {
        await(
          methodPerEndpointClient.validate(validationStruct, validationUnion, validationException))
      }
    }

    "Execute violationReturning method in MethodPerEndpoint with overriding method" in { _ =>
      val methodPerEndpoint = new ValidationService.MethodPerEndpoint with ServerValidationMixin {
        override def validate(
          structRequest: ValidationStruct,
          unionRequest: ValidationUnion,
          exceptionRequest: ValidationException
        ): Future[Boolean] = Future.False

        override def validateOption(
          structRequest: Option[ValidationStruct],
          unionRequest: Option[ValidationUnion],
          exceptionRequest: Option[ValidationException]
        ): Future[Boolean] = Future.False

        override def validateOnlyValidatedRequest(
          validationRequest: ValidationStruct
        ): Future[Boolean] = Future.False

        override def validateWithNonValidatedRequest(
          validationRequest: ValidationStruct,
          nonValidationRequest: NoValidationStruct
        ): Future[Boolean] = Future.False

        override def validateOnlyNonValidatedRequest(
          nonValidationRequest: NoValidationStruct
        ): Future[Boolean] = Future.False

        override def violationReturningValidate(
          structRequest: ValidationStruct,
          unionRequest: ValidationUnion,
          exceptionRequest: ValidationException,
          structRequestViolations: Set[ThriftValidationViolation],
          unionRequestViolations: Set[ThriftValidationViolation],
          exceptionRequestViolations: Set[ThriftValidationViolation]
        ): Future[Boolean] = {
          // if any of the request parameters has validation violations, return true, otherwise return false
          if (structRequestViolations.nonEmpty || unionRequestViolations.nonEmpty || exceptionRequestViolations.nonEmpty)
            Future.True
          else Future.False
        }

        override def violationReturningValidateOption(
          structRequest: Option[ValidationStruct],
          unionRequest: Option[ValidationUnion],
          exceptionRequest: Option[ValidationException],
          structRequestViolations: Set[ThriftValidationViolation],
          unionRequestViolations: Set[ThriftValidationViolation],
          exceptionRequestViolations: Set[ThriftValidationViolation]
        ): Future[Boolean] = {
          // if any of the request parameters has validation violations, return true, otherwise return false
          if (structRequestViolations.nonEmpty || unionRequestViolations.nonEmpty || exceptionRequestViolations.nonEmpty)
            Future.True
          else Future.False
        }

        override def violationReturningValidateWithNonValidatedRequest(
          validationRequest: ValidationStruct,
          noValidationRequest: NoValidationStruct,
          validationRequestViolations: Set[ThriftValidationViolation]
        ): Future[Boolean] = Future.value(validationRequestViolations.nonEmpty)

        override def violationReturningValidateOnlyValidatedRequest(
          validationRequest: ValidationStruct,
          validationRequestViolations: Set[ThriftValidationViolation]
        ): Future[Boolean] = Future.value(validationRequestViolations.nonEmpty)
      }

      val thriftServer =
        Thrift.server
          .serveIface(new InetSocketAddress(InetAddress.getLoopbackAddress, 0), methodPerEndpoint)

      val methodPerEndpointClient = Thrift.client.build[ValidationService.MethodPerEndpoint](
        Name.bound(Address(thriftServer.boundAddress.asInstanceOf[InetSocketAddress])),
        "client"
      )

      /******* validate endpoint *******/
      // all 3 inputs have violations, return true
      assert(
        await(methodPerEndpointClient
          .validate(validationStruct, validationUnion, validationException)))

      /******* validateOption endpoint *******/
      // all 3 inputs have violations, return true
      assert(await(methodPerEndpointClient
        .validateOption(Some(validationStruct), Some(validationUnion), Some(validationException))))
      // 2 inputs have violations (`None` has no violations), return true
      assert(
        await(methodPerEndpointClient
          .validateOption(Some(validationStruct), Some(validationUnion), None)))
      // all 3 inputs are `None`, invoking original method, return false
      assert(!await(methodPerEndpointClient.validateOption(None, None, None)))

      /******* validateOnlyValidatedRequest endpoint *******/
      assert(await(
        methodPerEndpointClient.validateOnlyValidatedRequest(validationRequest = validationStruct)))

      /******* validateWithNonValidatedRequest endpoint *******/
      assert(
        await(
          methodPerEndpointClient.validateWithNonValidatedRequest(
            validationRequest = validationStruct,
            nonValidationRequest = noValidationStruct)))

      /******* validateOnlyNonValidatedRequest endpoint *******/
      assert(
        !await(methodPerEndpointClient.validateOnlyNonValidatedRequest(nonValidationRequest =
          noValidationStruct)))
    }

    "Execute violationReturning method in MethodPerEndpoint WITHOUT overriding method" in { _ =>
      /******* validate endpoint *******/
      intercept[TApplicationException](
        await(methodPerEndpointClient
          .validate(validationStruct, validationUnion, validationException)))

      /******* validateOption endpoint *******/
      intercept[TApplicationException](await(methodPerEndpointClient
        .validateOption(Some(validationStruct), Some(validationUnion), Some(validationException))))
      intercept[TApplicationException](
        await(methodPerEndpointClient
          .validateOption(Some(validationStruct), Some(validationUnion), None)))
      // all 3 inputs are `None`, invoking original method, return true
      assert(await(methodPerEndpointClient.validateOption(None, None, None)))

      /******* validateOnlyValidatedRequest endpoint *******/
      intercept[TApplicationException](await(
        methodPerEndpointClient.validateOnlyValidatedRequest(validationRequest = validationStruct)))

      /******* validateWithNonValidatedRequest endpoint *******/
      intercept[TApplicationException](
        await(
          methodPerEndpointClient.validateWithNonValidatedRequest(
            validationRequest = validationStruct,
            nonValidationRequest = noValidationStruct)))

      /******* validateOnlyNonValidatedRequest endpoint *******/
      assert(
        await(methodPerEndpointClient.validateOnlyNonValidatedRequest(nonValidationRequest =
          noValidationStruct)))
    }

    "validate Option type with None and Some() request" in { _ =>
      intercept[TApplicationException] {
        await(methodPerEndpointClient
          .validateOption(Some(validationStruct), Some(validationUnion), Some(validationException)))
      }
      // check for option that has None as value
      // it shouldn't return an exception
      assert(await(methodPerEndpointClient.validateOption(None, None, None)))
    }

    "validate with Thrift client with servicePerEndpoint[ServicePerEndpoint]" in { _ =>
      val clientIface = Thrift.server.serveIface(
        new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
        methodPerEndpoint
      )
      val clientValidationService =
        Thrift.client.servicePerEndpoint[ValidationService.ServicePerEndpoint](
          Name.bound(Address(clientIface.boundAddress.asInstanceOf[InetSocketAddress])),
          "clientValidationService"
        )
      intercept[TApplicationException] {
        await(clientValidationService.validate(
          ValidationService.validate$args(validationStruct, validationUnion, validationException)))
      }
      intercept[TApplicationException] {
        await(
          clientValidationService.validateOption(
            ValidationService.validateOption$args(
              Some(validationStruct),
              Some(validationUnion),
              Some(validationException))))
      }
    }

    "validate with Thrift client with reqRepServiceEndPoint[ReqRepServiceEndPoint]" in { _ =>
      val clientIface = Thrift.server.serveIface(
        new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
        methodPerEndpoint
      )
      val clientValidationService =
        Thrift.client.servicePerEndpoint[ValidationService.ReqRepServicePerEndpoint](
          Name.bound(Address(clientIface.boundAddress.asInstanceOf[InetSocketAddress])),
          "clientValidationService"
        )
      intercept[TApplicationException] {
        await(clientValidationService.validate(Request(
          ValidationService.validate$args(validationStruct, validationUnion, validationException))))
      }
      intercept[TApplicationException] {
        await(
          clientValidationService.validateOption(
            Request(
              ValidationService.validateOption$args(
                Some(validationStruct),
                Some(validationUnion),
                Some(validationException)))))
      }
    }

    "validate if null parameters are passed as requests" in { _ =>
      //nullPointerException is handled in the mustache file
      assert(await(methodPerEndpointClient.validate(null, null, null)))
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
