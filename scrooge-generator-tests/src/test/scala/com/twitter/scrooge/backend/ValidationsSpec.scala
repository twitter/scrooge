package com.twitter.scrooge.backend

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.finagle.Address
import com.twitter.finagle.Name
import com.twitter.finagle.Thrift
import com.twitter.finagle.ThriftMux
import com.twitter.scrooge.Request
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

  def await[T](a: Awaitable[T], d: Duration = 5.seconds): T = Await.result(a, d)

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

  val validationException = ValidationException("")

  val validationUnion = ValidationUnion.IntField(-1)

  val iface = new ValidationService.MethodPerEndpoint {
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
  }

  val muxServer =
    ThriftMux.server.serveIface(new InetSocketAddress(InetAddress.getLoopbackAddress, 0), iface)

  val muxClient = ThriftMux.client.build[ValidationService.MethodPerEndpoint](
    Name.bound(Address(muxServer.boundAddress.asInstanceOf[InetSocketAddress])),
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
      val validationIntUnion = ValidationUnion.IntField(-1)
      val validationStringUnion = ValidationUnion.StringField("")
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
        await(muxClient.validate(validationStruct, validationUnion, validationException))
      }
    }

    "validate Option type with None and Some() request" in { _ =>
      intercept[TApplicationException] {
        await(muxClient
          .validateOption(Some(validationStruct), Some(validationUnion), Some(validationException)))
      }
      // check for option that has None as value
      // it shouldn't return an exception
      assert(await(muxClient.validateOption(None, None, None)))
    }

    "validate with Thrift client with servicePerEndpoint[ServicePerEndpoint]" in { _ =>
      val clientIface = Thrift.server.serveIface(
        new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
        iface
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
        iface
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
      assert(await(muxClient.validate(null, null, null)))
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
