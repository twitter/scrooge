package com.twitter.scrooge.backend

import java.util.Arrays
import org.specs.SpecificationWithJUnit
import org.specs.matcher.Matcher
import org.specs.mock.{ClassMocker, JMocker}
import org.apache.thrift.protocol._
import com.twitter.finagle
import com.twitter.finagle.thrift.ThriftClientRequest
import com.twitter.util.{Await, Future}
import com.twitter.scrooge.testutil.EvalHelper
import com.twitter.scrooge.ThriftException
import thrift.test._


class ServiceGeneratorSpec extends SpecificationWithJUnit with EvalHelper with JMocker with ClassMocker {
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
      val service: SimpleService[Some] = new SimpleService[Some] {
        def deliver(where: String) = Some(3)
      }

      service.deliver("Boston") mustEqual Some(3)
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

      SimpleService.deliver$args.decode(protocol).where mustEqual "boston"

      expect {
        startWrite(protocol, new TField("where", TType.STRING, 1))
        one(protocol).writeString("atlanta")
        endWrite(protocol)
      }

      SimpleService.deliver$args("atlanta").write(protocol) mustEqual ()

      expect {
        startRead(protocol, new TField("success", TType.I32, 0))
        one(protocol).readI32() willReturn 13
        endRead(protocol)
      }

      SimpleService.deliver$result.decode(protocol).success mustEqual Some(13)

      expect {
        startWrite(protocol, new TField("success", TType.I32, 0))
        one(protocol).writeI32(24)
        endWrite(protocol)
      }

      SimpleService.deliver$result(Some(24)).write(protocol) mustEqual ()
    }

    "generate unions for args and return value" in {
      expect {
        startRead(protocol, new TField("arg0", TType.STRUCT, 1))
        startRead(protocol, new TField("bools", TType.STRUCT, 2))
        startRead(protocol, new TField("im_true", TType.BOOL, 1))
        one(protocol).readBool() willReturn true
        nextRead(protocol, new TField("im_false", TType.BOOL, 2))
        one(protocol).readBool() willReturn false
        endRead(protocol)
        endRead(protocol)
        endRead(protocol)
      }

      ThriftTest.testUnions$args.decode(protocol).arg0 mustEqual
        MorePerfectUnion.Bools(Bools(true, false))

      expect {
        startWrite(protocol, new TField("arg0", TType.STRUCT, 1))
        startWrite(protocol, new TField("bonk", TType.STRUCT, 1))
        startWrite(protocol, new TField("message", TType.STRING, 1))
        one(protocol).writeString("hello world")
        nextWrite(protocol, new TField("type", TType.I32, 2))
        one(protocol).writeI32(42)
        endWrite(protocol)
        endWrite(protocol)
        endWrite(protocol)
      }

      ThriftTest.testUnions$args(
        MorePerfectUnion.Bonk(Bonk("hello world", 42))
      ).write(protocol) mustEqual ()

      expect {
        startRead(protocol, new TField("success", TType.STRUCT, 0))
        startRead(protocol, new TField("bools", TType.STRUCT, 2))
        startRead(protocol, new TField("im_true", TType.BOOL, 1))
        one(protocol).readBool() willReturn true
        nextRead(protocol, new TField("im_false", TType.BOOL, 2))
        one(protocol).readBool() willReturn false
        endRead(protocol)
        endRead(protocol)
        endRead(protocol)
      }

      ThriftTest.testUnions$result.decode(protocol).success mustEqual
        Some(MorePerfectUnion.Bools(Bools(true, false)))

      expect {
        startWrite(protocol, new TField("success", TType.STRUCT, 0))
        startWrite(protocol, new TField("bonk", TType.STRUCT, 1))
        startWrite(protocol, new TField("message", TType.STRING, 1))
        one(protocol).writeString("hello world")
        nextWrite(protocol, new TField("type", TType.I32, 2))
        one(protocol).writeI32(42)
        endWrite(protocol)
        endWrite(protocol)
        endWrite(protocol)
      }

      ThriftTest.testUnions$result(
        Some(MorePerfectUnion.Bonk(Bonk("hello world", 42)))
      ).write(protocol) mustEqual ()
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

      val res = ExceptionalService.deliver$result.decode(protocol)
      res.success must beNone
      res.ex must beSome(Xception(1, "silly"))
      res.ex2 must beNone

      expect {
        startWrite(protocol, new TField("success", TType.I32, 0))
        one(protocol).writeI32(24)
        endWrite(protocol)
      }

      ExceptionalService.deliver$result(Some(24)).write(protocol) mustEqual ()

      expect {
        startWrite(protocol, new TField("ex", TType.STRUCT, 1))
        startWrite(protocol, new TField("errorCode", TType.I32, 1))
        one(protocol).writeI32(1)
        nextWrite(protocol, new TField("message", TType.STRING, 2))
        one(protocol).writeString("silly")
        endWrite(protocol)
        endWrite(protocol)
      }

      ExceptionalService.deliver$result(None, Some(Xception(1, "silly"))).write(protocol) mustEqual ()

      expect {
        startWrite(protocol, new TField("ex3", TType.STRUCT, 3))
        one(protocol).writeStructBegin(capturingParam[TStruct].capture)
        one(protocol).writeFieldStop()
        one(protocol).writeStructEnd()
        endWrite(protocol)
      }
      ExceptionalService.deliver$result(None, None, None, Some(EmptyXception())).write(protocol) mustEqual ()
    }

    "generate FinagleService" in {
      val impl = mock[ExceptionalService[Future]]
      val service = new ExceptionalService$FinagleService(impl, new TBinaryProtocol.Factory)

      "success" in {
        val request = encodeRequest("deliver", ExceptionalService.deliver$args("Boston")).message
        val response = encodeResponse("deliver", ExceptionalService.deliver$result(success = Some(42)))

        expect {
          one(impl).deliver("Boston") willReturn Future.value(42)
        }

        service(request)() must matchByteArray(response)
      }

      "exception" in {
        val request = encodeRequest("deliver", ExceptionalService.deliver$args("Boston")).message
        val ex = Xception(1, "boom")
        val response = encodeResponse("deliver", ExceptionalService.deliver$result(ex = Some(ex)))

        expect {
          one(impl).deliver("Boston") willReturn Future.exception(ex)
        }

        service(request)() must matchByteArray(response)
      }
    }

    "generate FinagledClient" in {
      val impl = mock[ExceptionalService[Future]]
      val service = new ExceptionalService$FinagleService(impl, new TBinaryProtocol.Factory)
      val clientService = new finagle.Service[ThriftClientRequest, Array[Byte]] {
        def apply(req: ThriftClientRequest) = service(req.message)
      }
      val client = new ExceptionalService$FinagleClient(clientService)

      "success" in {
        val request = encodeRequest("deliver", ExceptionalService.deliver$args("Boston"))
        val response = encodeResponse("deliver", ExceptionalService.deliver$result(success = Some(42)))

        expect {
          one(impl).deliver("Boston") willReturn Future.value(42)
        }

        client.deliver("Boston")() mustEqual 42
      }

      "success void" in {
        val request = encodeRequest("remove", ExceptionalService.remove$args(123))
        val response = encodeResponse("remove", ExceptionalService.remove$result())

        expect {
          one(impl).remove(123) willReturn Future.Done
        }

        client.remove(123)() mustEqual ()
      }

      "exception" in {
        val ex = Xception(1, "boom")

        expect {
          one(impl).deliver("Boston") willReturn Future.exception(ex)
        }
        assert(Xception(1, "boom") == ex)
        Await.result(client.deliver("Boston")) must throwA(Xception(1, "boom"))
      }

      "void exception" in {
        val ex = Xception(1, "boom")

        expect {
          one(impl).remove(123) willReturn Future.exception(ex)
        }

        Await.result(client.remove(123)) must throwA[ThriftException](ex)
      }
    }

    "correctly inherit traits across services" in {
      "generic" in {
        class BasicImpl extends ReadWriteService[Some] {
          def getName() = Some("Rus")
          def setName(name: String) = Some(())
        }

        new BasicImpl() must haveSuperClass[ReadOnlyService[Some]]
        new BasicImpl() must haveSuperClass[ReadWriteService[Some]]
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
        val service = new ReadWriteService$FinagleService(null, null)
        service must haveSuperClass[ReadOnlyService$FinagleService]

        val client = new ReadWriteService$FinagleClient(null, null)
        client must haveSuperClass[ReadOnlyService$FinagleClient]
        client must haveSuperClass[ReadOnlyService[Future]]
        client must haveSuperClass[ReadWriteService[Future]]
      }
    }

    "camelize names only in the scala bindings" in {
      val service = new Capsly$FinagleService(null, null) {
        def getFunction2(name: String) = functionMap(name)
      }
      service.getFunction2("Bad_Name") must not(be_==(None))
    }
  }
}
