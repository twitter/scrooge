package com.twitter.scrooge.backend

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.finagle.Address
import com.twitter.finagle.Name
import com.twitter.finagle.Thrift
import com.twitter.finagle.stats.InMemoryStatsReceiver
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
import org.scalatest.OneInstancePerTest

class ValidationsSpec extends JMockSpec with OneInstancePerTest {

  def await[T](a: Awaitable[T], d: Duration = 100.seconds): T = Await.result(a, d)

  val invalidStructRequest =
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

  val validStructRequest =
    ValidationStruct(
      "email@gmail.com",
      1,
      99,
      -1,
      1,
      Map("1" -> "1"),
      boolField = true,
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

  val invalidExceptionRequest = ValidationException("")
  val validExceptionRequest = ValidationException("something")

  val invalidationUnionRequest = ValidationUnion.UnionIntField(-1)
  val validationUnionRequest = ValidationUnion.UnionIntField(1)

  val methodPerEndpoint = new ValidationService.MethodPerEndpoint {
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

    override def validateNestedRequest(
      nestedNonRequest: NestedNonValidationStruct
    ): Future[Boolean] = Future.True

    override def validateDeepNestedRequest(
      deepNestedRequest: DeepNestedValidationstruct
    ): Future[Boolean] =
      Future.True
  }

  val clientReceiver = new InMemoryStatsReceiver
  val serverReceiver = new InMemoryStatsReceiver

  val thriftServer = Thrift.server
    .withStatsReceiver(serverReceiver).serveIface(
      new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
      methodPerEndpoint)

  val methodPerEndpointClient = Thrift.client
    .withStatsReceiver(clientReceiver).build[ValidationService.MethodPerEndpoint](
      Name.bound(Address(thriftServer.boundAddress.asInstanceOf[InetSocketAddress])),
      "client")

  "validate nestedStruct with annotation in the innerStruct" in { _ =>
    val nestedNonValidationStruct = NestedNonValidationStruct("whatever", invalidStructRequest)
    intercept[TApplicationException] {
      await(methodPerEndpointClient.validateNestedRequest(nestedNonValidationStruct))
    }
  }

  "validate deepNestedStruct with annotation in the deepInnerStruct" in { _ =>
    val nestedNonValidationStruct = NestedNonValidationStruct("whatever", invalidStructRequest)
    val deepNestedValidationStruct =
      DeepNestedValidationstruct("whatever", nestedNonValidationStruct)
    intercept[TApplicationException] {
      await(methodPerEndpointClient.validateDeepNestedRequest(deepNestedValidationStruct))
    }
  }

  "validateInstanceValue" should {
    "validate Struct" in { _ =>
      val validationViolations = ValidationStruct.validateInstanceValue(invalidStructRequest)
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
        invalidStructRequest,
        Seq(invalidStructRequest, invalidStructRequest))
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
      val validationViolations = ValidationException.validateInstanceValue(invalidExceptionRequest)
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
          methodPerEndpointClient
            .validate(invalidStructRequest, invalidationUnionRequest, invalidExceptionRequest))
      }
    }

    "verify thriftServer and thriftClient are populated correctly in statsReceiver" in { _ =>
      intercept[TApplicationException] {
        await(
          methodPerEndpointClient
            .validate(invalidStructRequest, invalidationUnionRequest, invalidExceptionRequest))
      }
      assert(
        serverReceiver.counters(
          Seq(
            "thrift_validation",
            "violation",
            "validate",
            "com.twitter.scrooge.backend.thriftscala.ValidationStruct$Immutable")) == 1)
      assert(
        clientReceiver.counters(
          Seq("client", "validate", "failures", "org.apache.thrift.TApplicationException")) == 1)
    }

    "Execute violationReturning method in MethodPerEndpoint with overriding method" in { _ =>
      val methodPerEndpoint = new ServerValidationMixin {
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

        override def validateNestedRequest(
          nestedNonRequest: NestedNonValidationStruct
        ): Future[Boolean] = Future.False

        override def validateDeepNestedRequest(
          deepNestedRequest: DeepNestedValidationstruct
        ): Future[Boolean] =
          Future.False
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
          .validate(invalidStructRequest, invalidationUnionRequest, invalidExceptionRequest)))

      /******* validateOption endpoint *******/
      // all 3 inputs have violations, return true
      assert(
        await(
          methodPerEndpointClient
            .validateOption(
              Some(invalidStructRequest),
              Some(invalidationUnionRequest),
              Some(invalidExceptionRequest))))
      // 2 inputs have violations (`None` has no violations), return true
      assert(
        await(methodPerEndpointClient
          .validateOption(Some(invalidStructRequest), Some(invalidationUnionRequest), None)))
      // all 3 inputs are `None`, invoking original method, return false
      assert(!await(methodPerEndpointClient.validateOption(None, None, None)))

      /******* validateOnlyValidatedRequest endpoint *******/
      assert(
        await(methodPerEndpointClient.validateOnlyValidatedRequest(validationRequest =
          invalidStructRequest)))

      /******* validateWithNonValidatedRequest endpoint *******/
      assert(
        await(
          methodPerEndpointClient.validateWithNonValidatedRequest(
            validationRequest = invalidStructRequest,
            noValidationRequest = noValidationStruct)))

      /******* validateOnlyNonValidatedRequest endpoint *******/
      assert(
        !await(methodPerEndpointClient.validateOnlyNonValidatedRequest(noValidationRequest =
          noValidationStruct)))
    }

    "Execute original methods in MethodPerEndpoint withOUT implementing violationReturning method by extending ServerValidationMixin" in {
      _ =>
        val methodPerEndpoint = new ServerValidationMixin {
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

          override def validateNestedRequest(
            nestedNonRequest: NestedNonValidationStruct
          ): Future[Boolean] = Future.False

          override def validateDeepNestedRequest(
            deepNestedRequest: DeepNestedValidationstruct
          ): Future[Boolean] =
            Future.False
        }

        val thriftServer =
          Thrift.server
            .serveIface(new InetSocketAddress(InetAddress.getLoopbackAddress, 0), methodPerEndpoint)

        val methodPerEndpointClient = Thrift.client.build[ValidationService.MethodPerEndpoint](
          Name.bound(Address(thriftServer.boundAddress.asInstanceOf[InetSocketAddress])),
          "client"
        )

        /******* validate endpoint *******/
        // all 3 inputs have violations, throw exception
        intercept[TApplicationException](
          await(methodPerEndpointClient
            .validate(invalidStructRequest, invalidationUnionRequest, invalidExceptionRequest)))
        // all 3 request params are valid, execute original API, return false
        assert(
          !await(methodPerEndpointClient
            .validate(validStructRequest, validationUnionRequest, validExceptionRequest)))

        /******* validateOption endpoint *******/
        // all 3 inputs have violations, throw exception
        intercept[TApplicationException](
          await(
            methodPerEndpointClient
              .validateOption(
                Some(invalidStructRequest),
                Some(invalidationUnionRequest),
                Some(invalidExceptionRequest))))
        // 2 inputs have violations (`None` has no violations), throw exception
        intercept[TApplicationException](
          await(methodPerEndpointClient
            .validateOption(Some(invalidStructRequest), Some(invalidationUnionRequest), None)))
        // all 3 inputs are `None`, invoking original method, return false
        assert(!await(methodPerEndpointClient.validateOption(None, None, None)))

        /******* validateOnlyValidatedRequest endpoint *******/
        // invalid request, throw exception
        intercept[TApplicationException](
          await(methodPerEndpointClient.validateOnlyValidatedRequest(validationRequest =
            invalidStructRequest)))

        /******* validateWithNonValidatedRequest endpoint *******/
        // invalid request, throw exception
        intercept[TApplicationException](
          await(
            methodPerEndpointClient.validateWithNonValidatedRequest(
              validationRequest = invalidStructRequest,
              noValidationRequest = noValidationStruct)))

        /******* validateOnlyNonValidatedRequest endpoint *******/
        // no violations, execute original method, return false
        assert(
          !await(methodPerEndpointClient.validateOnlyNonValidatedRequest(noValidationRequest =
            noValidationStruct)))
    }

    "Execute violationReturning method in MethodPerEndpoint WITHOUT overriding method" in { _ =>
      /******* validate endpoint *******/
      intercept[TApplicationException](
        await(methodPerEndpointClient
          .validate(invalidStructRequest, invalidationUnionRequest, invalidExceptionRequest)))

      /******* validateOption endpoint *******/
      intercept[TApplicationException](
        await(
          methodPerEndpointClient
            .validateOption(
              Some(invalidStructRequest),
              Some(invalidationUnionRequest),
              Some(invalidExceptionRequest))))
      intercept[TApplicationException](
        await(methodPerEndpointClient
          .validateOption(Some(invalidStructRequest), Some(invalidationUnionRequest), None)))
      // all 3 inputs are `None`, invoking original method, return true
      assert(await(methodPerEndpointClient.validateOption(None, None, None)))

      /******* validateOnlyValidatedRequest endpoint *******/
      intercept[TApplicationException](
        await(methodPerEndpointClient.validateOnlyValidatedRequest(validationRequest =
          invalidStructRequest)))

      /******* validateWithNonValidatedRequest endpoint *******/
      intercept[TApplicationException](
        await(
          methodPerEndpointClient.validateWithNonValidatedRequest(
            validationRequest = invalidStructRequest,
            noValidationRequest = noValidationStruct)))

      /******* validateOnlyNonValidatedRequest endpoint *******/
      assert(
        await(methodPerEndpointClient.validateOnlyNonValidatedRequest(noValidationRequest =
          noValidationStruct)))
    }

    "validate Option type with None and Some() request" in { _ =>
      intercept[TApplicationException] {
        await(
          methodPerEndpointClient
            .validateOption(
              Some(invalidStructRequest),
              Some(invalidationUnionRequest),
              Some(invalidExceptionRequest)))
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
        await(clientValidationService.validate(ValidationService
          .validate$args(invalidStructRequest, invalidationUnionRequest, invalidExceptionRequest)))
      }
      intercept[TApplicationException] {
        await(
          clientValidationService.validateOption(
            ValidationService.validateOption$args(
              Some(invalidStructRequest),
              Some(invalidationUnionRequest),
              Some(invalidExceptionRequest))))
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
        await(clientValidationService.validate(Request(ValidationService
          .validate$args(invalidStructRequest, invalidationUnionRequest, invalidExceptionRequest))))
      }
      intercept[TApplicationException] {
        await(
          clientValidationService.validateOption(
            Request(
              ValidationService.validateOption$args(
                Some(invalidStructRequest),
                Some(invalidationUnionRequest),
                Some(invalidExceptionRequest)))))
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
