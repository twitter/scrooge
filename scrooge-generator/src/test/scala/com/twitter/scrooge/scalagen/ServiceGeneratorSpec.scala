package com.twitter.scrooge
package scalagen

import java.util.Arrays
import com.twitter.finagle
import com.twitter.finagle.thrift.ThriftClientRequest
import com.twitter.util.Future
import org.specs.Specification
import org.specs.matcher.Matcher
import org.specs.mock.{ClassMocker, JMocker}
import org.apache.thrift.protocol._
import thrift.test._

class ServiceGeneratorSpec extends Specification with EvalHelper with JMocker with ClassMocker {
  import AST._

  val protocol = mock[TProtocol]

  case class matchThriftClientRequest(r: ThriftClientRequest) extends Matcher[ThriftClientRequest]() {
    def apply(v: => ThriftClientRequest) = (
      areEqual(v, r),
      "okMessage",
      "no match")

    def areEqual(a: ThriftClientRequest, b: ThriftClientRequest) =
      Arrays.equals(a.message, b.message) && a.oneway == b.oneway
  }

  case class matchByteArray(bs: Array[Byte]) extends Matcher[Array[Byte]]() {
    def apply(v: => Array[Byte]) = (
      Arrays.equals(v, bs),
      "okMessage",
      "no match")
  }

  "ScalaGenerator service" should {
    "generate a service interface" in {
      val service: SimpleService.Iface = new SimpleService.Iface {
        def deliver(where: String) = 3
      }

      service.deliver("Boston") mustEqual 3
    }

    "generate a future-based service interface" in {
      val service: SimpleService.FutureIface = new SimpleService.FutureIface {
        def deliver(where: String) = Future(3)
      }

      service.deliver("Boston")() mustEqual 3
    }

    "generate structs for args and return value" in {
      expect {
        startRead(protocol, new TField("where", TType.STRING, 1))
        one(protocol).readString() willReturn "boston"
        endRead(protocol)
      }

      SimpleService.deliver_args(protocol).where mustEqual "boston"

      expect {
        startWrite(protocol, new TField("where", TType.STRING, 1))
        one(protocol).writeString("atlanta")
        endWrite(protocol)
      }

      SimpleService.deliver_args("atlanta").write(protocol) mustEqual ()

      expect {
        startRead(protocol, new TField("success", TType.I32, 0))
        one(protocol).readI32() willReturn 13
        endRead(protocol)
      }

      SimpleService.deliver_result(protocol).success mustEqual Some(13)

      expect {
        startWrite(protocol, new TField("success", TType.I32, 0))
        one(protocol).writeI32(24)
        endWrite(protocol)
      }

      SimpleService.deliver_result(Some(24)).write(protocol) mustEqual ()
    }

    "generate exception return values" in {
      expect {
        startRead(protocol, new TField("ex", TType.STRUCT, 1))
        startRead(protocol, new TField("errorCode", TType.I32, 1))
        one(protocol).readI32() willReturn 1
        nextRead(protocol, new TField("message", TType.STRING, 2))
        one(protocol).readString() willReturn "silly"
        endRead(protocol)
        endRead(protocol)
      }

      val res = ExceptionalService.deliver_result(protocol)
      res.success must beNone
      res.ex must beSome(Xception(1, "silly"))
      res.ex2 must beNone

      expect {
        startWrite(protocol, new TField("success", TType.I32, 0))
        one(protocol).writeI32(24)
        endWrite(protocol)
      }

      ExceptionalService.deliver_result(Some(24)).write(protocol) mustEqual ()

      expect {
        startWrite(protocol, new TField("ex", TType.STRUCT, 1))
        startWrite(protocol, new TField("errorCode", TType.I32, 1))
        one(protocol).writeI32(1)
        nextWrite(protocol, new TField("message", TType.STRING, 2))
        one(protocol).writeString("silly")
        endWrite(protocol)
        endWrite(protocol)
      }

      ExceptionalService.deliver_result(None, Some(Xception(1, "silly"))).write(protocol) mustEqual ()
    }

    "generate FinagledService" in {
      val impl = mock[ExceptionalService.FutureIface]
      val service = new ExceptionalService.FinagledService(impl, new TBinaryProtocol.Factory)

      "success" in {
        val request = encodeRequest("deliver", ExceptionalService.deliver_args("Boston")).message
        val response = encodeResponse("deliver", ExceptionalService.deliver_result(success = Some(42)))

        expect {
          one(impl).deliver("Boston") willReturn Future.value(42)
        }

        service(request)() must matchByteArray(response)
      }

      "exception" in {
        val request = encodeRequest("deliver", ExceptionalService.deliver_args("Boston")).message
        val ex = Xception(1, "boom")
        val response = encodeResponse("deliver", ExceptionalService.deliver_result(ex = Some(ex)))

        expect {
          one(impl).deliver("Boston") willReturn Future.exception(ex)
        }

        service(request)() must matchByteArray(response)
      }
    }

    "generate FinagledClient" in {
      val impl = mock[ExceptionalService.FutureIface]
      val service = new ExceptionalService.FinagledService(impl, new TBinaryProtocol.Factory)
      val clientService = new finagle.Service[ThriftClientRequest, Array[Byte]] {
        def apply(req: ThriftClientRequest) = service(req.message)
      }
      val client = new ExceptionalService.FinagledClient(clientService)

      "success" in {
        val request = encodeRequest("deliver", ExceptionalService.deliver_args("Boston"))
        val response = encodeResponse("deliver", ExceptionalService.deliver_result(success = Some(42)))

        expect {
          one(impl).deliver("Boston") willReturn Future.value(42)
        }

        client.deliver("Boston")() mustEqual 42
      }

      "exception" in {
        val request = encodeRequest("deliver", ExceptionalService.deliver_args("Boston"))
        val ex = Xception(1, "boom")
        val response = encodeResponse("deliver", ExceptionalService.deliver_result(ex = Some(ex)))

        expect {
          one(impl).deliver("Boston") willReturn Future.exception(ex)
        }

        client.deliver("Boston")() must throwA(ex)
      }
    }

    "correctly inherit traits across services" in {
      "synchronous" in {
        class BasicImpl extends ReadWriteService.Iface {
          def getName() = "Rus"
          def setName(name: String) { }
        }

        new BasicImpl() must haveSuperClass[ReadOnlyService.Iface]
        new BasicImpl() must haveSuperClass[ReadWriteService.Iface]
      }

      "future-based" in {
        class FutureImpl extends ReadWriteService.FutureIface {
          def getName() = Future("Rus")
          def setName(name: String) = Future.Unit
        }

        new FutureImpl() must haveSuperClass[ReadOnlyService.FutureIface]
        new FutureImpl() must haveSuperClass[ReadWriteService.FutureIface]
      }

      "finagle" in {
        val service = new ReadWriteService.FinagledService(null, null)
        service must haveSuperClass[ReadOnlyService.FinagledService]

        val client = new ReadWriteService.FinagledClient(null, null)
        client must haveSuperClass[ReadOnlyService.FinagledClient]
        client must haveSuperClass[ReadOnlyService.FutureIface]
        client must haveSuperClass[ReadWriteService.FutureIface]
      }
    }

    "camelize names only in the scala bindings" in {
      val service = new Capsly.FinagledService(null, null) {
        def getFunction2(name: String) = functionMap(name)
      }
      service.getFunction2("Bad_Name") must not(be_==(None))
    }
  }
}
