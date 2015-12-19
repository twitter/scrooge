package com.twitter.scrooge.backend

import com.twitter.finagle
import com.twitter.finagle.param.{Stats, Label}
import com.twitter.finagle.{ListeningServer, Name, Thrift, Service, SimpleFilter, SourcedException}
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finagle.thrift.{ThriftClientRequest, ThriftServiceIface}
import com.twitter.scrooge.{ThriftStruct, ThriftException}
import com.twitter.scrooge.testutil.{EvalHelper, JMockSpec}
import com.twitter.util.{Await, Future, Return}
import java.net.{InetAddress, InetSocketAddress}
import org.apache.thrift.protocol._
import org.jmock.Expectations.{any, returnValue}
import org.jmock.lib.legacy.ClassImposteriser
import org.jmock.{Expectations, Mockery}
import scala.language.reflectiveCalls
import thrift.test._
import thrift.test.ExceptionalService._


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

    "generate correct defaults" in { _ =>
      val service = new Defaults.FutureIface {
        def defaultParams(
          arg1: Int,
          arg2: Option[Int],
          arg3: Int,
          arg4: Seq[Int],
          arg5: Option[Seq[Int]],
          arg6: Boolean
        ): Future[Int] = Future.value(arg2.getOrElse(-1))
      }

      // the other parameters should have defaults.
      val result = service.defaultParams(arg1 = 999)
      Await.result(result) must be(-1)
    }

    "generate structs for args and return value" in { cycle => import cycle._
      val protocol = mock[TProtocol]

      expecting { e => import e._
        startRead(e, protocol, new TField("where", TType.STRING, 1))
        one(protocol).readString(); will(returnValue("boston"))
        endRead(e, protocol)
      }

      whenExecuting {
        SimpleService.Deliver.Args.decode(protocol).where must be("boston")
      }

      expecting { e => import e._
        startWrite(e, protocol, new TField("where", TType.STRING, 1))
        one(protocol).writeString("atlanta")
        endWrite(e, protocol)
      }

      whenExecuting {
        SimpleService.Deliver.Args("atlanta").write(protocol) must be(())
      }

      expecting { e => import e._
        startRead(e, protocol, new TField("success", TType.I32, 0))
        one(protocol).readI32(); will(returnValue(13))
        endRead(e, protocol)
      }

      whenExecuting {
        SimpleService.Deliver.Result.decode(protocol).success must be(Some(13))
      }

      expecting { e => import e._
        startWrite(e, protocol, new TField("success", TType.I32, 0))
        one(protocol).writeI32(24)
        endWrite(e, protocol)
      }

      whenExecuting {
        SimpleService.Deliver.Result(Some(24)).write(protocol) must be(())
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
        ThriftTest.TestUnions.Args.decode(protocol).arg0 must be(
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
        ThriftTest.TestUnions.Args(
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
        ThriftTest.TestUnions.Result.decode(protocol).success must be(
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
        ThriftTest.TestUnions.Result(
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
        val res = ExceptionalService.Deliver.Result.decode(protocol)
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
        ExceptionalService.Deliver.Result(Some(24)).write(protocol) must be(())
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
        ExceptionalService.Deliver.Result(None, Some(Xception(1, "silly"))).write(protocol) must be(())
      }

      expecting { e => import e._
        startWrite(e, protocol, new TField("ex3", TType.STRUCT, 3))
        one(protocol).writeStructBegin(`with`(any(classOf[TStruct])))
        one(protocol).writeFieldStop()
        one(protocol).writeStructEnd()
        endWrite(e, protocol)
      }

      whenExecuting {
        ExceptionalService.Deliver.Result(None, None, None, Some(EmptyXception())).write(protocol) must be(())
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
        val request = encodeRequest("deliver", ExceptionalService.Deliver.Args("Boston")).message
        val response = encodeResponse("deliver", ExceptionalService.Deliver.Result(success = Some(42)))

        context.checking(new Expectations {
          one(impl).deliver("Boston"); will(returnValue(Future.value(42)))
        })

        Await.result(service(request)) must be(response)
        context.assertIsSatisfied()
      }

      "exception" in { _ =>
        val request = encodeRequest("deliver", ExceptionalService.Deliver.Args("Boston")).message
        val ex = Xception(1, "boom")
        val response = encodeResponse("deliver", ExceptionalService.Deliver.Result(ex = Some(ex)))

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
      val client = new ExceptionalService$FinagleClient(clientService, serviceName="ExceptionalService")

      "set service name" in { _ =>
        client.serviceName must be("ExceptionalService")
      }

      "success" in { _ =>
        context.checking(new Expectations {
          one(impl).deliver("Boston"); will(returnValue(Future.value(42)))
        })

        Await.result(client.deliver("Boston")) must be(42)
        context.assertIsSatisfied()
      }

      "success void" in { _ =>
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

      "source exception" in { _ =>
        val ex = new SourcedException {}

        context.checking(new Expectations {
          one(impl).deliver("Boston"); will(returnValue(Future.exception(ex)))
        })

        val e = intercept[SourcedException] {
          Await.result(client.deliver("Boston"))
        }
        e.serviceName must be("ExceptionalService")
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

    "generate a finagle Service per method" should {
      "work for basic services" in { _ =>
        import SimpleService._

        val server = Thrift.serveIface(
          new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
          new SimpleService[Future] {
            def deliver(where: String) = Future.value(3)
          })

        val simpleService: SimpleService.ServiceIface =
          Thrift.newServiceIface[SimpleService.ServiceIface](Name.bound(server.boundAddress), "simple")
        val simpleServiceIface = Thrift.newMethodIface(simpleService)
        Await.result(simpleService.deliver(Deliver.Args("Boston")).map { result =>
          result.success
        }) must be(Some(3))
      }

      "work for inherited services" in { _ =>
        import ReadOnlyService._
        import ReadWriteService._
        val server = Thrift.serveIface(
          new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
          new ReadWriteService[Future] {
            private[this] var name = "Initial name"
            def getName(): Future[String] = Future.value(name)

            def setName(newName: String): Future[Unit] = {
              name = newName
              Future.Done
            }
          }
        )

        val readOnlyClientService = Thrift.newServiceIface[ReadOnlyService.ServiceIface](Name.bound(server.boundAddress), "read-only")
        Await.result(readOnlyClientService.getName(GetName.Args()).map(_.success)) must be (Some("Initial name"))

        val readWriteClientService = Thrift.newServiceIface[ReadWriteService.ServiceIface](Name.bound(server.boundAddress), "read-write")
        Await.result(readWriteClientService.getName(GetName.Args()).map(_.success)) must be (Some("Initial name"))

        Await.result(readWriteClientService.setName(SetName.Args("New name"))) must be(SetName.Result())

        Await.result(readOnlyClientService.getName(GetName.Args()).map(_.success)) must be(Some("New name"))
      }

      def serveExceptionalService(): ListeningServer = Thrift.serveIface(
        new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
        new ExceptionalService[Future] {
          private[this] var counter = 0

          def deliver(where: String): Future[Int] = {
            counter = counter + 1
            if (where.isEmpty)
              Future.exception(new EmptyXception)
            else if (counter % 3 != 0)
              Future.exception(new Xception(0, "Try again"))
            else
              Future.value(123)
          }

          def remove(id: Int): Future[Unit] = Future.Done
        }
      )

      "work with exceptions" in { _ =>
        val server = serveExceptionalService()

        val clientService = Thrift.newServiceIface[ExceptionalService.ServiceIface](Name.bound(server.boundAddress), "client")

        Await.result(clientService.deliver(Deliver.Args("")).map(_.ex3)) must be (Some(new EmptyXception()))
      }

      "work with filters on args" in { _ =>
        import SimpleService._
        val server = Thrift.serveIface(
          new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
          new SimpleService[Future] {
            def deliver(input: String) = Future.value(input.length)
          })

        val simpleServiceIface: SimpleService.ServiceIface =
          Thrift.newServiceIface[SimpleService.ServiceIface](Name.bound(server.boundAddress), "simple")

        val doubleFilter = new SimpleFilter[Deliver.Args, Deliver.Result] {
          def apply(args: Deliver.Args, service: Service[Deliver.Args, Deliver.Result]) =
            service(args.copy(where = args.where + args.where))
        }

        val filteredServiceIface = simpleServiceIface.copy(deliver = doubleFilter andThen simpleServiceIface.deliver)
        val methodIface = Thrift.newMethodIface(filteredServiceIface)
        Await.result(methodIface.deliver("123")) must be(6)
      }

      "work with retrying filters" in { _ =>
        import com.twitter.finagle.service.{RetryExceptionsFilter, RetryPolicy}
        import com.twitter.util.{JavaTimer, Try, Throw}

        val service = serveExceptionalService()
        val clientService = Thrift.newServiceIface[ExceptionalService.ServiceIface](Name.bound(service.boundAddress), "client")

        val retryPolicy =
          RetryPolicy.tries[Try[Int]](3, {
            case Throw(ex) if ex.getMessage == "Try again" =>
              true
          })

        val retriedDeliveryService =
          new RetryExceptionsFilter(retryPolicy, new JavaTimer(true)) andThen
            ThriftServiceIface.resultFilter(Deliver) andThen
            clientService.deliver
        Await.result(retriedDeliveryService(Deliver.Args("there"))) must be (123)
      }

      "work with a newMethodIface" in { _ =>
        val service = serveExceptionalService()
        val clientService = Thrift.newServiceIface[ExceptionalService.ServiceIface](Name.bound(service.boundAddress), "client")

        val futureIface = Thrift.newMethodIface(clientService)

        intercept[EmptyXception] {
          Await.result(futureIface.deliver(""))
        }
      }

      "work with both camelCase and snake_case function names" in { _ =>
        CamelCaseSnakeCaseService.FooBar.name mustBe "foo_bar"
        CamelCaseSnakeCaseService.BazQux.name mustBe "bazQux"

        val server = Thrift.serveIface(
          new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
          new CamelCaseSnakeCaseService[Future] {
            def fooBar(fooBar: String): Future[String] = Future.value(fooBar)
            def bazQux(bazQux: String): Future[String] = Future.value(bazQux)
          }
        )

        val client = Thrift.newServiceIface[CamelCaseSnakeCaseService.ServiceIface](Name.bound(server.boundAddress), "client")
        val richClient = Thrift.newMethodIface(client)

        Await.result(richClient.fooBar("foo")) mustBe "foo"
        Await.result(richClient.bazQux("baz")) mustBe "baz"
      }

      "have correct stats" in { _ =>
        val service = serveExceptionalService()
        val statsReceiver = new InMemoryStatsReceiver
        val clientService = Thrift.client.
          configured(Label("customServiceName")).
          configured(Stats(statsReceiver)).
          newServiceIface[ExceptionalService.ServiceIface](Name.bound(service.boundAddress), "client")

        val futureIface = Thrift.newMethodIface(clientService)

        intercept[Xception] {
          Await.result(futureIface.deliver(where = "abc"))
        }

        statsReceiver.counters(Seq("customServiceName", "ExceptionalService", "deliver", "requests")) must be (1)
        statsReceiver.counters(Seq("customServiceName", "ExceptionalService", "deliver", "failures")) must be (1)
        statsReceiver.counters(Seq("customServiceName", "ExceptionalService", "deliver", "failures", "thrift.test.Xception")) must be (1)

        intercept[Xception] {
          Await.result(futureIface.deliver(where = "abc"))
        }

        // The 3rd request succeeds
        Await.result(futureIface.deliver(where = "abc"))

        statsReceiver.counters(Seq("customServiceName", "ExceptionalService", "deliver", "requests")) must be (3)
        statsReceiver.counters(Seq("customServiceName", "ExceptionalService", "deliver", "success")) must be (1)
        statsReceiver.counters(Seq("customServiceName", "ExceptionalService", "deliver", "failures")) must be (2)
        statsReceiver.counters(Seq("customServiceName", "ExceptionalService", "deliver", "failures", "thrift.test.Xception")) must be (2)
      }

      "have stats with serviceName not set" in { _ =>
        val service = serveExceptionalService()
        val statsReceiver = new InMemoryStatsReceiver
        val clientService = Thrift.client.configured(Stats(statsReceiver)).
          newServiceIface[ExceptionalService.ServiceIface](Name.bound(service.boundAddress), "client")

        val futureIface = Thrift.newMethodIface(clientService)

        intercept[Xception] {
          Await.result(futureIface.deliver(where = "abc"))
        }

        statsReceiver.counters(Seq("ExceptionalService", "deliver", "requests")) must be (1)
      }
    }

    "generate eponymous Service" should {

      val context = new Mockery
      context.setImposteriser(ClassImposteriser.INSTANCE)
      val impl = context.mock(classOf[thrift.test.Service[Future]])
      val service = new thrift.test.Service$FinagleService(impl, new TBinaryProtocol.Factory)

      "allow generation and calls to eponymous FinagledService" in { _ =>

        context.checking(new Expectations {
          one(impl).test(); will(returnValue(Future.value(())))
        })

        val request = encodeRequest("test", thrift.test.Service.Test.Args()).message
        val response = encodeResponse("test", thrift.test.Service.Test.Result())

        Await.result(service(request)) mustBe response

        context.assertIsSatisfied()
      }

      "allow generation and calls to eponymous FinagledClient" in { _ =>

        val clientService = new finagle.Service[ThriftClientRequest, Array[Byte]] {
          def apply(req: ThriftClientRequest) = service(req.message)
        }

        val client = new thrift.test.Service$FinagleClient(clientService, serviceName = "Service")

        context.checking(new Expectations {
          one(impl).test(); will(returnValue(Future.Done))
        })

        Await.result(client.test()) mustBe ((): Unit)
        context.assertIsSatisfied()
      }
    }
  }
}
