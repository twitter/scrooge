package com.twitter.scrooge.backend

import com.twitter.finagle
import com.twitter.finagle.thrift.ThriftClientRequest
import com.twitter.scrooge.ThriftException
import com.twitter.scrooge.testutil.{JMockSpec, EvalHelper}
import com.twitter.util.{Await, Future}
import org.apache.thrift.protocol._
import org.jmock.{Expectations, Mockery}
import org.jmock.Expectations.{any, returnValue}
import org.jmock.lib.legacy.ClassImposteriser
import thrift.test._


class ServiceGeneratorSpec extends JMockSpec with EvalHelper {
  "ScalaGenerator service" should {
    "generate a service interface" in { _ =>
      val service: SimpleService[Some] = new SimpleService[Some] {
        def deliver(where: String) = Some(3)
      }

      service.deliver("Boston") must be(Some(3))
    }

    "generate a future-based service interface" in { _ =>
      val service: SimpleService.FutureIface = new SimpleService.FutureIface {
        def deliver(where: String) = Future(3)
      }

      Await.result(service.deliver("Boston")) must be(3)
    }

    "generate structs for args and return value" in { cycle => import cycle._
      val protocol = mock[TProtocol]

      expecting { e => import e._
        startRead(e, protocol, new TField("where", TType.STRING, 1))
        one(protocol).readString(); will(returnValue("boston"))
        endRead(e, protocol)
      }

      whenExecuting {
        SimpleService.deliver$args.decode(protocol).where must be("boston")
      }

      expecting { e => import e._
        startWrite(e, protocol, new TField("where", TType.STRING, 1))
        one(protocol).writeString("atlanta")
        endWrite(e, protocol)
      }

      whenExecuting {
        SimpleService.deliver$args("atlanta").write(protocol) must be(())
      }

      expecting { e => import e._
        startRead(e, protocol, new TField("success", TType.I32, 0))
        one(protocol).readI32(); will(returnValue(13))
        endRead(e, protocol)
      }

      whenExecuting {
        SimpleService.deliver$result.decode(protocol).success must be(Some(13))
      }

      expecting { e => import e._
        startWrite(e, protocol, new TField("success", TType.I32, 0))
        one(protocol).writeI32(24)
        endWrite(e, protocol)
      }

      whenExecuting {
        SimpleService.deliver$result(Some(24)).write(protocol) must be(())
      }
    }

    "generate unions for args and return value" in { cycle => import cycle._
      val protocol = mock[TProtocol]

      expecting { e => import e._
        startRead(e, protocol, new TField("arg0", TType.STRUCT, 1))
        startRead(e, protocol, new TField("bools", TType.STRUCT, 2))
        startRead(e, protocol, new TField("im_true", TType.BOOL, 1))
        one(protocol).readBool(); will(returnValue(true))
        nextRead(e, protocol, new TField("im_false", TType.BOOL, 2))
        one(protocol).readBool(); will(returnValue(false))
        endRead(e, protocol)
        endRead(e, protocol)
        endRead(e, protocol)
      }

      whenExecuting {
        ThriftTest.testUnions$args.decode(protocol).arg0 must be(
          MorePerfectUnion.Bools(Bools(true, false)))
      }

      expecting { e => import e._
        startWrite(e, protocol, new TField("arg0", TType.STRUCT, 1))
        startWrite(e, protocol, new TField("bonk", TType.STRUCT, 1))
        startWrite(e, protocol, new TField("message", TType.STRING, 1))
        one(protocol).writeString(`with`(Expectations.equal("hello world")))
        nextWrite(e, protocol, new TField("type", TType.I32, 2))
        one(protocol).writeI32(`with`(Expectations.equal(42)))
        endWrite(e, protocol)
        endWrite(e, protocol)
        endWrite(e, protocol)
      }

      whenExecuting {
        ThriftTest.testUnions$args(
          MorePerfectUnion.Bonk(Bonk("hello world", 42))
        ).write(protocol) must be(())
      }

      expecting { e => import e._
        startRead(e, protocol, new TField("success", TType.STRUCT, 0))
        startRead(e, protocol, new TField("bools", TType.STRUCT, 2))
        startRead(e, protocol, new TField("im_true", TType.BOOL, 1))
        one(protocol).readBool(); will(returnValue(true))
        nextRead(e, protocol, new TField("im_false", TType.BOOL, 2))
        one(protocol).readBool(); will(returnValue(false))
        endRead(e, protocol)
        endRead(e, protocol)
        endRead(e, protocol)
      }

      whenExecuting {
        ThriftTest.testUnions$result.decode(protocol).success must be(
          Some(MorePerfectUnion.Bools(Bools(true, false))))
      }

      expecting { e => import e._
        startWrite(e, protocol, new TField("success", TType.STRUCT, 0))
        startWrite(e, protocol, new TField("bonk", TType.STRUCT, 1))
        startWrite(e, protocol, new TField("message", TType.STRING, 1))
        one(protocol).writeString(`with`(Expectations.equal("hello world")))
        nextWrite(e, protocol, new TField("type", TType.I32, 2))
        one(protocol).writeI32(`with`(Expectations.equal(42)))
        endWrite(e, protocol)
        endWrite(e, protocol)
        endWrite(e, protocol)
      }

      whenExecuting {
        ThriftTest.testUnions$result(
          Some(MorePerfectUnion.Bonk(Bonk("hello world", 42)))
        ).write(protocol) must be(())
      }
    }

    "generate exception return values" in { cycle => import cycle._
      val protocol = mock[TProtocol]

      expecting { e => import e._
        startRead(e, protocol, new TField("ex", TType.STRUCT, 1))
        startRead(e, protocol, new TField("errorCode", TType.I32, 1))
        one(protocol).readI32(); will(returnValue(1))
        nextRead(e, protocol, new TField("message", TType.STRING, 2))
        one(protocol).readString(); will(returnValue("silly"))
        endRead(e, protocol)
        endRead(e, protocol)
      }

      whenExecuting {
        val res = ExceptionalService.deliver$result.decode(protocol)
        res.success must be(None)
        res.ex must be(Some(Xception(1, "silly")))
        res.ex2 must be(None)
      }

      expecting { e => import e._
        startWrite(e, protocol, new TField("success", TType.I32, 0))
        one(protocol).writeI32(24)
        endWrite(e, protocol)
      }

      whenExecuting {
        ExceptionalService.deliver$result(Some(24)).write(protocol) must be(())
      }

      expecting { e => import e._
        startWrite(e, protocol, new TField("ex", TType.STRUCT, 1))
        startWrite(e, protocol, new TField("errorCode", TType.I32, 1))
        one(protocol).writeI32(`with`(Expectations.equal(1)))
        nextWrite(e, protocol, new TField("message", TType.STRING, 2))
        one(protocol).writeString(`with`(Expectations.equal("silly")))
        endWrite(e, protocol)
        endWrite(e, protocol)
      }

      whenExecuting {
        ExceptionalService.deliver$result(None, Some(Xception(1, "silly"))).write(protocol) must be(())
      }

      expecting { e => import e._
        startWrite(e, protocol, new TField("ex3", TType.STRUCT, 3))
        one(protocol).writeStructBegin(`with`(any(classOf[TStruct])))
        one(protocol).writeFieldStop()
        one(protocol).writeStructEnd()
        endWrite(e, protocol)
      }

      whenExecuting {
        ExceptionalService.deliver$result(None, None, None, Some(EmptyXception())).write(protocol) must be(())
      }
    }

    "generate FinagleService" should {
      // use JMock manually - the scalatest JMock integration has trouble with
      // the erasure for ExceptionalService[Future]
      val context = new Mockery
      context.setImposteriser(ClassImposteriser.INSTANCE)
      val impl = context.mock(classOf[ExceptionalService[Future]])
      val service = new ExceptionalService$FinagleService(impl, new TBinaryProtocol.Factory)

      "success" in { _ =>
        val request = encodeRequest("deliver", ExceptionalService.deliver$args("Boston")).message
        val response = encodeResponse("deliver", ExceptionalService.deliver$result(success = Some(42)))

        context.checking(new Expectations {
          one(impl).deliver("Boston"); will(returnValue(Future.value(42)))
        })

        Await.result(service(request)) must be(response)
        context.assertIsSatisfied()
      }

      "exception" in { _ =>
        val request = encodeRequest("deliver", ExceptionalService.deliver$args("Boston")).message
        val ex = Xception(1, "boom")
        val response = encodeResponse("deliver", ExceptionalService.deliver$result(ex = Some(ex)))

        context.checking(new Expectations {
          one(impl).deliver("Boston"); will(returnValue(Future.exception(ex)))
        })

        Await.result(service(request)) must be(response)
        context.assertIsSatisfied()
      }
    }

    "generate FinagledClient" should {
      val context = new Mockery
      context.setImposteriser(ClassImposteriser.INSTANCE)
      val impl = context.mock(classOf[ExceptionalService[Future]])
      val service = new ExceptionalService$FinagleService(impl, new TBinaryProtocol.Factory)
      val clientService = new finagle.Service[ThriftClientRequest, Array[Byte]] {
        def apply(req: ThriftClientRequest) = service(req.message)
      }
      val client = new ExceptionalService$FinagleClient(clientService)

      "success" in { _ =>
        val request = encodeRequest("deliver", ExceptionalService.deliver$args("Boston"))
        val response = encodeResponse("deliver", ExceptionalService.deliver$result(success = Some(42)))

        context.checking(new Expectations {
          one(impl).deliver("Boston"); will(returnValue(Future.value(42)))
        })

        Await.result(client.deliver("Boston")) must be(42)
        context.assertIsSatisfied()
      }

      "success void" in { _ =>
        val request = encodeRequest("remove", ExceptionalService.remove$args(123))
        val response = encodeResponse("remove", ExceptionalService.remove$result())

        context.checking(new Expectations {
          one(impl).remove(123); will(returnValue(Future.Done))
        })

        Await.result(client.remove(123)) must be(())
        context.assertIsSatisfied()
      }

      "exception" in { _ =>
        val ex = Xception(1, "boom")

        context.checking(new Expectations {
          one(impl).deliver("Boston"); will(returnValue(Future.exception(ex)))
        })

        assert(Xception(1, "boom") == ex)
        val e = intercept[Xception] {
          Await.result(client.deliver("Boston"))
        }
        e must be(Xception(1, "boom"))
        context.assertIsSatisfied()
      }

      "void exception" in { _ =>
        val ex = Xception(1, "boom")

        context.checking(new Expectations {
          one(impl).remove(123); will(returnValue(Future.exception(ex)))
        })

        val e = intercept[ThriftException] {
          Await.result(client.remove(123))
        }
        e must be(ex)
        context.assertIsSatisfied()
      }
    }

    "correctly inherit traits across services" should {
      "generic" in { _ =>
        class BasicImpl extends ReadWriteService[Some] {
          def getName() = Some("Rus")
          def setName(name: String) = Some(())
        }

        new BasicImpl().isInstanceOf[ReadOnlyService[Some]] must be(true)
        new BasicImpl().isInstanceOf[ReadWriteService[Some]] must be(true)
      }

      "future-based" in { _ =>
        class FutureImpl extends ReadWriteService.FutureIface {
          def getName() = Future("Rus")
          def setName(name: String) = Future.Unit
        }

        new FutureImpl().isInstanceOf[ReadOnlyService.FutureIface] must be(true)
        new FutureImpl().isInstanceOf[ReadWriteService.FutureIface] must be(true)
      }

      "finagle" in { _ =>
        val service = new ReadWriteService$FinagleService(null, null)
        service.isInstanceOf[ReadOnlyService$FinagleService] must be(true)

        val client = new ReadWriteService$FinagleClient(null, null)
        client.isInstanceOf[ReadOnlyService$FinagleClient] must be(true)
        client.isInstanceOf[ReadOnlyService[Future]] must be(true)
        client.isInstanceOf[ReadWriteService[Future]] must be(true)
      }
    }

    "camelize names only in the scala bindings" in { _ =>
      val service = new Capsly$FinagleService(null, null) {
        def getFunction2(name: String) = functionMap(name)
      }
      service.getFunction2("Bad_Name") must not be(None)
    }
  }
}
