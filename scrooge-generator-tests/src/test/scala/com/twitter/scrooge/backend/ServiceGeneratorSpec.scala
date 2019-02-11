package com.twitter.scrooge.backend

import _root_.thrift.test.ExceptionalService._
import _root_.thrift.test._
import collisions.dupes.thriftscala.{Aaa, Ccc}
import com.twitter.conversions.DurationOps._
import com.twitter.finagle
import com.twitter.finagle.param.Stats
import com.twitter.finagle.service.{ReqRep, ResponseClass, ResponseClassifier}
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finagle.thrift.{RichClientParam, RichServerParam, ThriftClientRequest}
import com.twitter.finagle.{Service => finagleService, _}
import com.twitter.scrooge.testutil.{EvalHelper, JMockSpec}
import com.twitter.scrooge.{Request, Response, ThriftException}
import com.twitter.util.{Await, Future, Return, Time}
import java.net.{InetAddress, InetSocketAddress}
import java.util.concurrent.atomic.AtomicBoolean
import org.apache.thrift.protocol._
import org.apache.thrift.transport.TMemoryInputTransport
import org.jmock.AbstractExpectations.{any, returnValue}
import org.jmock.lib.legacy.ClassImposteriser
import org.jmock.{Expectations, Mockery}
import org.scalatest.concurrent.Eventually
import scala.language.reflectiveCalls

class ServiceGeneratorSpec extends JMockSpec with EvalHelper with Eventually {
  "ScalaGenerator service" should {
    "generate a service interface" in { _ =>
      val service: SimpleService[Some] = new SimpleService[Some] {
        def deliver(where: String) = Some(3)
      }

      service.deliver("Boston") must be(Some(3))
    }

    "generate a future-based service interface" in { _ =>
      val service: SimpleService.MethodPerEndpoint = new SimpleService.MethodPerEndpoint {
        def deliver(where: String) = Future(3)
      }

      Await.result(service.deliver("Boston")) must be(3)
    }

    "generate correct defaults" in { _ =>
      val service = new Defaults.MethodPerEndpoint {
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

    "generate structs for args and return value" in { cycle =>
      import cycle._
      val protocol = mock[TProtocol]

      expecting { e =>
        import e._
        startRead(e, protocol, new TField("where", TType.STRING, 1))
        e.oneOf(protocol).readString(); will(returnValue("boston"))
        endRead(e, protocol)
      }

      whenExecuting {
        SimpleService.Deliver.Args.decode(protocol).where must be("boston")
      }

      expecting { e =>
        startWrite(e, protocol, new TField("where", TType.STRING, 1))
        e.oneOf(protocol).writeString("atlanta")
        endWrite(e, protocol)
      }

      whenExecuting {
        SimpleService.Deliver.Args("atlanta").write(protocol) must be(())
      }

      expecting { e =>
        import e._
        startRead(e, protocol, new TField("success", TType.I32, 0))
        e.oneOf(protocol).readI32(); will(returnValue(13))
        endRead(e, protocol)
      }

      whenExecuting {
        SimpleService.Deliver.Result.decode(protocol).success must be(Some(13))
      }

      expecting { e =>
        startWrite(e, protocol, new TField("success", TType.I32, 0))
        e.oneOf(protocol).writeI32(24)
        endWrite(e, protocol)
      }

      whenExecuting {
        SimpleService.Deliver.Result(Some(24)).write(protocol) must be(())
      }
    }

    "generate unions for args and return value" in { cycle =>
      import cycle._
      val protocol = mock[TProtocol]

      expecting { e =>
        import e._
        startRead(e, protocol, new TField("arg0", TType.STRUCT, 1))
        startRead(e, protocol, new TField("bools", TType.STRUCT, 2))
        startRead(e, protocol, new TField("im_true", TType.BOOL, 1))
        e.oneOf(protocol).readBool(); will(returnValue(true))
        nextRead(e, protocol, new TField("im_false", TType.BOOL, 2))
        e.oneOf(protocol).readBool(); will(returnValue(false))
        endRead(e, protocol)
        endRead(e, protocol)
        endRead(e, protocol)
      }

      whenExecuting {
        ThriftTest.TestUnions.Args.decode(protocol).arg0 must be(
          MorePerfectUnion.Bools(Bools(true, false))
        )
      }

      expecting { e =>
        import e._
        startWrite(e, protocol, new TField("arg0", TType.STRUCT, 1))
        startWrite(e, protocol, new TField("bonk", TType.STRUCT, 1))
        startWrite(e, protocol, new TField("message", TType.STRING, 1))
        e.oneOf(protocol).writeString(`with`("hello world"))
        nextWrite(e, protocol, new TField("type", TType.I32, 2))
        e.oneOf(protocol).writeI32(`with`(42))
        endWrite(e, protocol)
        endWrite(e, protocol)
        endWrite(e, protocol)
      }

      whenExecuting {
        ThriftTest.TestUnions
          .Args(
            MorePerfectUnion.Bonk(Bonk("hello world", 42))
          )
          .write(protocol) must be(())
      }

      expecting { e =>
        import e._
        startRead(e, protocol, new TField("success", TType.STRUCT, 0))
        startRead(e, protocol, new TField("bools", TType.STRUCT, 2))
        startRead(e, protocol, new TField("im_true", TType.BOOL, 1))
        e.oneOf(protocol).readBool(); will(returnValue(true))
        nextRead(e, protocol, new TField("im_false", TType.BOOL, 2))
        e.oneOf(protocol).readBool(); will(returnValue(false))
        endRead(e, protocol)
        endRead(e, protocol)
        endRead(e, protocol)
      }

      whenExecuting {
        ThriftTest.TestUnions.Result.decode(protocol).success must be(
          Some(MorePerfectUnion.Bools(Bools(true, false)))
        )
      }

      expecting { e =>
        import e._
        startWrite(e, protocol, new TField("success", TType.STRUCT, 0))
        startWrite(e, protocol, new TField("bonk", TType.STRUCT, 1))
        startWrite(e, protocol, new TField("message", TType.STRING, 1))
        e.oneOf(protocol).writeString(`with`("hello world"))
        nextWrite(e, protocol, new TField("type", TType.I32, 2))
        e.oneOf(protocol).writeI32(`with`(42))
        endWrite(e, protocol)
        endWrite(e, protocol)
        endWrite(e, protocol)
      }

      whenExecuting {
        ThriftTest.TestUnions
          .Result(
            Some(MorePerfectUnion.Bonk(Bonk("hello world", 42)))
          )
          .write(protocol) must be(())
      }
    }

    "generate exception return values" in { cycle =>
      import cycle._
      val protocol = mock[TProtocol]

      expecting { e =>
        import e._
        startRead(e, protocol, new TField("ex", TType.STRUCT, 1))
        startRead(e, protocol, new TField("errorCode", TType.I32, 1))
        e.oneOf(protocol).readI32(); will(returnValue(1))
        nextRead(e, protocol, new TField("message", TType.STRING, 2))
        e.oneOf(protocol).readString(); will(returnValue("silly"))
        endRead(e, protocol)
        endRead(e, protocol)
      }

      whenExecuting {
        val res = ExceptionalService.Deliver.Result.decode(protocol)
        res.success must be(None)
        res.ex must be(Some(Xception(1, "silly")))
        res.ex2 must be(None)
      }

      expecting { e =>
        startWrite(e, protocol, new TField("success", TType.I32, 0))
        e.oneOf(protocol).writeI32(24)
        endWrite(e, protocol)
      }

      whenExecuting {
        ExceptionalService.Deliver.Result(Some(24)).write(protocol) must be(())
      }

      expecting { e =>
        import e._
        startWrite(e, protocol, new TField("ex", TType.STRUCT, 1))
        startWrite(e, protocol, new TField("errorCode", TType.I32, 1))
        e.oneOf(protocol).writeI32(`with`(1))
        nextWrite(e, protocol, new TField("message", TType.STRING, 2))
        e.oneOf(protocol).writeString(`with`("silly"))
        endWrite(e, protocol)
        endWrite(e, protocol)
      }

      whenExecuting {
        ExceptionalService.Deliver.Result(None, Some(Xception(1, "silly"))).write(protocol) must be(
          ()
        )
      }

      expecting { e =>
        import e._
        startWrite(e, protocol, new TField("ex3", TType.STRUCT, 3))
        e.oneOf(protocol).writeStructBegin(`with`(any(classOf[TStruct])))
        e.oneOf(protocol).writeFieldStop()
        e.oneOf(protocol).writeStructEnd()
        endWrite(e, protocol)
      }

      whenExecuting {
        ExceptionalService.Deliver
          .Result(None, None, None, Some(EmptyXception()))
          .write(protocol) must be(())
      }
    }

    "generate FinagleService" should {
      // use JMock manually - the scalatest JMock integration has trouble with
      // the erasure for ExceptionalService[Future]
      val context = new Mockery
      context.setImposteriser(ClassImposteriser.INSTANCE)
      val impl = context.mock(classOf[ExceptionalService[Future]])
      val service = new ExceptionalService$FinagleService(impl, RichServerParam())

      "success" in { _ =>
        val request = encodeRequest("deliver", ExceptionalService.Deliver.Args("Boston")).message
        val response =
          encodeResponse("deliver", ExceptionalService.Deliver.Result(success = Some(42)))

        context.checking(new Expectations {
          this.oneOf(impl).deliver("Boston"); will(returnValue(Future.value(42)))
        })

        Await.result(service(request)) must be(response)
        context.assertIsSatisfied()
      }

      "exception" in { _ =>
        val request = encodeRequest("deliver", ExceptionalService.Deliver.Args("Boston")).message
        val ex = Xception(1, "boom")
        val response = encodeResponse("deliver", ExceptionalService.Deliver.Result(ex = Some(ex)))

        context.checking(new Expectations {
          this.oneOf(impl).deliver("Boston"); will(returnValue(Future.exception(ex)))
        })

        Await.result(service(request)) must be(response)
        context.assertIsSatisfied()
      }
    }

    "generate Filter" should {
      val context = new Mockery
      context.setImposteriser(ClassImposteriser.INSTANCE)
      val protocolFactory = new TBinaryProtocol.Factory

      "basic usage" in { _ =>
        val request = encodeRequest("deliver", ExceptionalService.Deliver.Args("Boston")).message
        val response =
          encodeResponse("deliver", ExceptionalService.Deliver.Result(success = Some(42)))

        val impl = context.mock(
          classOf[
            finagleService[ExceptionalService.Deliver.Args, ExceptionalService.Deliver.SuccessType]
          ]
        )
        val filters = new ExceptionalService.Filter(RichServerParam())
        val svc = filters.deliver.andThen(impl)

        context.checking(new Expectations {
          this.oneOf(impl).apply(ExceptionalService.Deliver.Args("Boston"));
          will(returnValue(Future.value(42)))
        })

        // this initial deserialisation is normally done by the router
        val inputTransport = new TMemoryInputTransport(request)
        val iprot = protocolFactory.getProtocol(inputTransport)
        val msg = iprot.readMessageBegin()

        Await.result(svc((iprot, msg.seqid))) must be(response)
        context.assertIsSatisfied()
      }

      "inherited method" in { _ =>
        val request = encodeRequest("duplicated", Ccc.Duplicated.Args("ccc")).message
        val response =
          encodeResponse("duplicated", Ccc.Duplicated.Result(success = Some(42)))

        val impl = context.mock(
          classOf[
            finagleService[Ccc.Duplicated.Args, Ccc.Duplicated.SuccessType]
          ],
          "ccc"
        )
        val filters = new Ccc.Filter(RichServerParam())
        val svc = filters.duplicated.andThen(impl)

        context.checking(new Expectations {
          this.oneOf(impl).apply(Ccc.Duplicated.Args("ccc"));
          will(returnValue(Future.value(42)))
        })

        // this initial deserialisation is normally done by the router
        val inputTransport = new TMemoryInputTransport(request)
        val iprot = protocolFactory.getProtocol(inputTransport)
        val msg = iprot.readMessageBegin()

        Await.result(svc((iprot, msg.seqid))) must be(response)
        context.assertIsSatisfied()
      }

      "parent method" in { _ =>
        val request = encodeRequest("duplicated", Aaa.Duplicated.Args()).message
        val response =
          encodeResponse("duplicated", Aaa.Duplicated.Result(success = Some(42)))

        val impl = context.mock(
          classOf[
            finagleService[Aaa.Duplicated.Args, Aaa.Duplicated.SuccessType]
          ],
          "aaa"
        )
        val filters = new Aaa.Filter(RichServerParam())
        val svc = filters.duplicated.andThen(impl)

        context.checking(new Expectations {
          this.oneOf(impl).apply(Aaa.Duplicated.Args());
          will(returnValue(Future.value(42)))
        })

        // this initial deserialisation is normally done by the router
        val inputTransport = new TMemoryInputTransport(request)
        val iprot = protocolFactory.getProtocol(inputTransport)
        val msg = iprot.readMessageBegin()

        Await.result(svc((iprot, msg.seqid))) must be(response)
        context.assertIsSatisfied()
      }
    }

    "generate FinagledClient" should {
      val context = new Mockery
      context.setImposteriser(ClassImposteriser.INSTANCE)
      val impl = context.mock(classOf[ExceptionalService[Future]])
      val service = new ExceptionalService$FinagleService(impl, RichServerParam())
      val clientService = new finagle.Service[ThriftClientRequest, Array[Byte]] {
        def apply(req: ThriftClientRequest) = service(req.message)
      }
      val client =
        new ExceptionalService$FinagleClient(
          clientService,
          RichClientParam(serviceName = "ExceptionalService")
        )

      "set service name" in { _ =>
        client.serviceName must be("ExceptionalService")
      }

      "success" in { _ =>
        context.checking(new Expectations {
          this.oneOf(impl).deliver("Boston"); will(returnValue(Future.value(42)))
        })

        Await.result(client.deliver("Boston")) must be(42)
        context.assertIsSatisfied()
      }

      "success void" in { _ =>
        context.checking(new Expectations {
          this.oneOf(impl).remove(123); will(returnValue(Future.Done))
        })

        Await.result(client.remove(123)) must be(())
        context.assertIsSatisfied()
      }

      "exception" in { _ =>
        val ex = Xception(1, "boom")

        context.checking(new Expectations {
          this.oneOf(impl).deliver("Boston"); will(returnValue(Future.exception(ex)))
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
          this.oneOf(impl).deliver("Boston"); will(returnValue(Future.exception(ex)))
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
          this.oneOf(impl).remove(123); will(returnValue(Future.exception(ex)))
        })

        val e = intercept[ThriftException] {
          Await.result(client.remove(123))
        }
        e must be(ex)
        context.assertIsSatisfied()
      }

      "be closable" in { _ =>
        val closableService = new finagle.Service[ThriftClientRequest, Array[Byte]] {
          val testService = new ExceptionalService$FinagleService(impl, RichServerParam())
          val isOpen = new AtomicBoolean(true)

          def apply(req: ThriftClientRequest) = testService(req.message)

          override def status: Status = if (isOpen.get()) Status.Open else Status.Closed

          override def close(deadline: Time): Future[Unit] = {
            isOpen.set(false)
            Future.Unit
          }
        }

        val testClient = new ExceptionalService$FinagleClient(closableService, RichClientParam())

        closableService.status mustBe Status.Open
        Await.result(testClient.asClosable.close(), 2.seconds)
        closableService.status mustBe Status.Closed
      }

      "be closable MethodPerEndpoint" in { _ =>
        val service = Thrift.server.serveIface(
          new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
          new SimpleService[Future] {
            def deliver(input: String) = Future.value(input.length)
          }
        )

        val clientService = Thrift.client.servicePerEndpoint[SimpleService.ServicePerEndpoint](
          Name.bound(Address(service.boundAddress.asInstanceOf[InetSocketAddress])),
          "simple"
        )

        val clientMethod = Thrift.Client.methodPerEndpoint(clientService)

        Await.result(clientMethod.deliver("pass"), 2.seconds)
        Await.result(clientMethod.asClosable.close(), 2.seconds)

        intercept[ServiceClosedException](
          Await.result(clientMethod.deliver("pass"), 2.seconds)
        )
      }

      "be closable ServicePerEndpoint" in { _ =>
        val service = Thrift.server.serveIface(
          new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
          new SimpleService[Future] {
            def deliver(input: String) = Future.value(input.length)
          }
        )

        val clientService = Thrift.client.servicePerEndpoint[SimpleService.ServicePerEndpoint](
          Name.bound(Address(service.boundAddress.asInstanceOf[InetSocketAddress])),
          "simple"
        )

        Await.result(clientService.deliver(SimpleService.Deliver.Args("pass")), 2.seconds)
        Await.result(clientService.asClosable.close(), 2.seconds)

        intercept[ServiceClosedException](
          Await.result(clientService.deliver(SimpleService.Deliver.Args("pass")), 2.seconds)
        )
      }

      "be closable ReqRepServicePerEndpoint" in { _ =>
        import com.twitter.scrooge

        val service = Thrift.server.serveIface(
          new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
          new SimpleService[Future] {
            def deliver(input: String) = Future.value(input.length)
          }
        )

        val clientService =
          Thrift.client.servicePerEndpoint[SimpleService.ReqRepServicePerEndpoint](
            Name.bound(Address(service.boundAddress.asInstanceOf[InetSocketAddress])),
            "simple"
          )

        Await.result(
          clientService.deliver(scrooge.Request(SimpleService.Deliver.Args("pass"))),
          2.seconds
        )
        Await.result(clientService.asClosable.close(), 2.seconds)

        intercept[ServiceClosedException](
          Await.result(
            clientService.deliver(scrooge.Request(SimpleService.Deliver.Args("pass"))),
            2.seconds
          )
        )
      }

      // close is not a reserved method, closable is reserved
      // when users define their own asClosable, scrooge doesn't generate it
      "user can define close method and their own asClosable method" in { _ =>
        val closableService = Thrift.server.serveIface(
          new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
          new ClosableService[Future] {
            def close() = Future.value("close")
            def asClosable() = Future.value("asClosable")
          }
        )

        val closableClient = Thrift.client.build[ClosableService.MethodPerEndpoint](
          Name.bound(Address(closableService.boundAddress.asInstanceOf[InetSocketAddress])),
          "closableClient"
        )

        assert(Await.result(closableClient.close(), 5.seconds) == "close")
        assert(Await.result(closableClient.asClosable(), 5.seconds) == "asClosable")
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
        class FutureImpl extends ReadWriteService.MethodPerEndpoint {
          def getName() = Future("Rus")
          def setName(name: String) = Future.Unit
        }

        new FutureImpl().isInstanceOf[ReadOnlyService.MethodPerEndpoint] must be(true)
        new FutureImpl().isInstanceOf[ReadWriteService.MethodPerEndpoint] must be(true)
      }

      "finagle" in { _ =>
        val service = new ReadWriteService$FinagleService(null, RichServerParam())
        service.isInstanceOf[ReadOnlyService$FinagleService] must be(true)

        val client = new ReadWriteService$FinagleClient(null, RichClientParam())
        client.isInstanceOf[ReadOnlyService$FinagleClient] must be(true)
        client.isInstanceOf[ReadOnlyService[Future]] must be(true)
        client.isInstanceOf[ReadWriteService[Future]] must be(true)
      }
    }

    "camelize names only in the scala bindings" in { _ =>
      val service = new Capsly$FinagleService(null, RichServerParam()) {
        def getFunction2(name: String) = serviceMap(name)
      }
      service.getFunction2("Bad_Name") must not be (None)
    }

    "generate a finagle Service per method" should {
      "work for basic services" in { _ =>
        import SimpleService._

        val server = Thrift.server.serveIface(
          new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
          new SimpleService[Future] {
            def deliver(where: String) = Future.value(3)
          }
        )

        val simpleService: SimpleService.ServicePerEndpoint =
          Thrift.client.servicePerEndpoint[SimpleService.ServicePerEndpoint](
            Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
            "simple"
          )
        Await.result(simpleService.deliver(Deliver.Args("Boston")), 5.seconds) must be(3)

        Await.result(server.close(), 2.seconds)
      }

      "work for inherited services" in { _ =>
        import ReadOnlyService._
        import ReadWriteService._
        val server = Thrift.server.serveIface(
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

        val readOnlyClientService =
          Thrift.client.servicePerEndpoint[ReadOnlyService.ServicePerEndpoint](
            Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
            "read-only"
          )
        Await.result(readOnlyClientService.getName(GetName.Args()), 5.seconds) must be(
          "Initial name"
        )

        val readWriteClientService =
          Thrift.client.servicePerEndpoint[ReadWriteService.ServicePerEndpoint](
            Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
            "read-write"
          )
        Await.result(readWriteClientService.getName(GetName.Args()), 5.seconds) must be(
          "Initial name"
        )

        Await.result(readWriteClientService.setName(SetName.Args("New name")), 5.seconds) must be(
          ()
        )

        Await.result(readOnlyClientService.getName(GetName.Args()), 5.seconds) must be("New name")

        Await.result(server.close(), 2.seconds)
      }

      def serveExceptionalService(): ListeningServer = Thrift.server.serveIface(
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

        val clientService = Thrift.client.servicePerEndpoint[ExceptionalService.ServicePerEndpoint](
          Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
          "client"
        )

        intercept[EmptyXception] {
          Await.result(clientService.deliver(Deliver.Args("")), 5.seconds)
        }

        Await.result(server.close(), 2.seconds)
      }

      "work with filters on args" in { _ =>
        import ExceptionalService._
        val server = Thrift.server.serveIface(
          new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
          new ExceptionalService[Future] {
            def deliver(input: String) = Future.value(input.length)
            def remove(id: Int): Future[Unit] = Future.Done
          }
        )

        val exceptionalServiceIface: ExceptionalService.ServicePerEndpoint =
          Thrift.client.servicePerEndpoint[ExceptionalService.ServicePerEndpoint](
            Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
            "simple"
          )

        val doubleFilter = new SimpleFilter[Deliver.Args, Deliver.SuccessType] {
          def apply(
            args: Deliver.Args,
            service: finagleService[Deliver.Args, Deliver.SuccessType]
          ) =
            service(args.copy(where = args.where + args.where))
        }

        val filteredServiceIface =
          exceptionalServiceIface.withDeliver(
            deliver = doubleFilter.andThen(exceptionalServiceIface.deliver)
          )
        val methodIface = Thrift.Client.methodPerEndpoint(filteredServiceIface)
        Await.result(methodIface.deliver("123")) must be(6)

        Await.result(server.close(), 2.seconds)
      }

      "work with retrying filters" in { _ =>
        import com.twitter.finagle.service.{RetryExceptionsFilter, RetryPolicy}
        import com.twitter.util.{JavaTimer, Throw, Try}

        val service = serveExceptionalService()
        val clientService = Thrift.client.servicePerEndpoint[ExceptionalService.ServicePerEndpoint](
          Name.bound(Address(service.boundAddress.asInstanceOf[InetSocketAddress])),
          "client"
        )

        val retryPolicy =
          RetryPolicy.tries[Try[Int]](3, {
            case Throw(ex) if ex.getMessage == "Try again" =>
              true
          })

        val retriedDeliveryService =
          new RetryExceptionsFilter(retryPolicy, new JavaTimer(true)) andThen
            clientService.deliver
        Await.result(retriedDeliveryService(Deliver.Args("there"))) must be(123)

        Await.result(service.close(), 2.seconds)
      }

      "work with a methodPerEndpoint" in { _ =>
        val service = serveExceptionalService()
        val clientService = Thrift.client.servicePerEndpoint[ExceptionalService.ServicePerEndpoint](
          Name.bound(Address(service.boundAddress.asInstanceOf[InetSocketAddress])),
          "client"
        )

        val futureIface = Thrift.Client.methodPerEndpoint(clientService)

        intercept[EmptyXception] {
          Await.result(futureIface.deliver(""))
        }

        Await.result(service.close(), 2.seconds)
      }

      "work with both camelCase and snake_case function names" in { _ =>
        CamelCaseSnakeCaseService.FooBar.name mustBe "foo_bar"
        CamelCaseSnakeCaseService.BazQux.name mustBe "bazQux"

        val server = Thrift.server.serveIface(
          new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
          new CamelCaseSnakeCaseService[Future] {
            def fooBar(fooBar: String): Future[String] = Future.value(fooBar)
            def bazQux(bazQux: String): Future[String] = Future.value(bazQux)
          }
        )

        val client = Thrift.client.servicePerEndpoint[CamelCaseSnakeCaseService.ServicePerEndpoint](
          Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
          "client"
        )
        val richClient = Thrift.Client.methodPerEndpoint(client)

        Await.result(richClient.fooBar("foo")) mustBe "foo"
        Await.result(richClient.bazQux("baz")) mustBe "baz"

        Await.result(server.close(), 2.seconds)
      }

      "have correct stats" in { _ =>
        val service = serveExceptionalService()
        val statsReceiver = new InMemoryStatsReceiver
        val clientService = Thrift.client
          .configured(Stats(statsReceiver))
          .withPerEndpointStats
          .servicePerEndpoint[ExceptionalService.ServicePerEndpoint](
            Name.bound(Address(service.boundAddress.asInstanceOf[InetSocketAddress])),
            "customServiceName"
          )

        val futureIface = Thrift.Client.methodPerEndpoint(clientService)

        intercept[Xception] {
          Await.result(futureIface.deliver(where = "abc"))
        }

        eventually {
          statsReceiver.counters(
            Seq("customServiceName", "ExceptionalService", "deliver", "requests")
          ) must be(1)
        }
        eventually {
          statsReceiver.counters(
            Seq("customServiceName", "ExceptionalService", "deliver", "failures")
          ) must be(1)
        }
        eventually {
          statsReceiver.counters(
            Seq(
              "customServiceName",
              "ExceptionalService",
              "deliver",
              "failures",
              "thrift.test.Xception"
            )
          ) must be(1)
        }

        intercept[Xception] {
          Await.result(futureIface.deliver(where = "abc"))
        }

        // The 3rd request succeeds
        Await.result(futureIface.deliver(where = "abc"))

        eventually {
          statsReceiver.counters(
            Seq("customServiceName", "ExceptionalService", "deliver", "requests")
          ) must be(3)
        }
        eventually {
          statsReceiver.counters(
            Seq("customServiceName", "ExceptionalService", "deliver", "success")
          ) must be(1)
        }
        eventually {
          statsReceiver.counters(
            Seq("customServiceName", "ExceptionalService", "deliver", "failures")
          ) must be(2)
        }
        eventually {
          statsReceiver.counters(
            Seq(
              "customServiceName",
              "ExceptionalService",
              "deliver",
              "failures",
              "thrift.test.Xception"
            )
          ) must be(2)
        }

        Await.result(service.close(), 2.seconds)
      }

      "have correct stats with ResponseClassifier" in { _ =>
        val server: ListeningServer = Thrift.server.serveIface(
          new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
          new SimpleService[Future] {
            def deliver(where: String): Future[Int] = Future.value(where.length)
          }
        )

        val bigNumsAreFailures: ResponseClassifier = {
          case ReqRep(_, Return(i: Int)) if i >= 3 => ResponseClass.NonRetryableFailure
          case ReqRep(_, Return(i: Int)) => ResponseClass.Success
        }
        val stats = new InMemoryStatsReceiver
        val clientService = Thrift.client
          .withStatsReceiver(stats)
          .withResponseClassifier(bigNumsAreFailures)
          .newService(
            Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
            "client"
          )

        val svc = new SimpleService.FinagledClient(
          clientService,
          RichClientParam(
            clientStats = stats,
            serviceName = "SimpleService",
            responseClassifier = bigNumsAreFailures
          )
        )

        val requests = stats.counter("SimpleService", "deliver", "requests")
        val success = stats.counter("SimpleService", "deliver", "success")
        val failures = stats.counter("SimpleService", "deliver", "failures")

        Await.result(svc.deliver("abcd"), 5.seconds) // length 4 is a failure
        eventually { assert(1 == requests()) }
        eventually { assert(0 == success()) }
        eventually { assert(1 == failures()) }

        Await.result(svc.deliver("ab"), 5.seconds) // length 2 is ok
        eventually { assert(2 == requests()) }
        eventually { assert(1 == success()) }
        eventually { assert(1 == failures()) }

        Await.result(server.close(), 2.seconds)
      }

      "have stats with serviceName not set" in { _ =>
        val service = serveExceptionalService()
        val statsReceiver = new InMemoryStatsReceiver
        val clientService = Thrift.client
          .configured(Stats(statsReceiver))
          .withPerEndpointStats
          .servicePerEndpoint[ExceptionalService.ServicePerEndpoint](
            Name.bound(Address(service.boundAddress.asInstanceOf[InetSocketAddress])),
            "client"
          )

        val futureIface = Thrift.Client.methodPerEndpoint(clientService)

        intercept[Xception] {
          Await.result(futureIface.deliver(where = "abc"))
        }

        eventually {
          statsReceiver.counters(Seq("client", "ExceptionalService", "deliver", "requests")) must be(
            1
          )
        }

        Await.result(service.close(), 2.seconds)
      }
    }

    "generate a finagle Req/Rep Service per method (ThriftMux only)" should {
      "work for basic services" in { _ =>
        import SimpleService._

        val server = ThriftMux.server.serveIface(
          new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
          new SimpleService[Future] {
            def deliver(where: String) = Future.value(3)
          }
        )

        val simpleService: SimpleService.ReqRepServicePerEndpoint =
          ThriftMux.client.servicePerEndpoint[SimpleService.ReqRepServicePerEndpoint](
            Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
            "simple"
          )
        Await
          .result(simpleService.deliver(Request(Deliver.Args("Boston"))), 5.seconds).value must be(
          3
        )

        Await.result(server.close(), 2.seconds)
      }

      "work for inherited services" in { _ =>
        import ReadOnlyService._
        import ReadWriteService._
        val server = ThriftMux.server.serveIface(
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

        val readOnlyClientService =
          ThriftMux.client.servicePerEndpoint[ReadOnlyService.ReqRepServicePerEndpoint](
            Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
            "read-only"
          )
        Await
          .result(readOnlyClientService.getName(Request(GetName.Args())), 5.seconds).value must be(
          "Initial name"
        )

        val readWriteClientService =
          ThriftMux.client.servicePerEndpoint[ReadWriteService.ReqRepServicePerEndpoint](
            Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
            "read-write"
          )
        Await
          .result(readWriteClientService.getName(Request(GetName.Args())), 5.seconds).value must be(
          "Initial name"
        )

        Await
          .result(readWriteClientService.setName(Request(SetName.Args("New name"))), 5.seconds).value must be(
          ()
        )

        Await
          .result(readOnlyClientService.getName(Request(GetName.Args())), 5.seconds).value must be(
          "New name"
        )

        Await.result(server.close(), 2.seconds)
      }

      def serveExceptionalService(): ListeningServer = ThriftMux.server.serveIface(
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

        val clientService =
          ThriftMux.client.servicePerEndpoint[ExceptionalService.ReqRepServicePerEndpoint](
            Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
            "client"
          )

        intercept[EmptyXception] {
          Await.result(clientService.deliver(Request(Deliver.Args(""))), 5.seconds)
        }

        Await.result(server.close(), 2.seconds)
      }

      "work with filters on args" in { _ =>
        import ExceptionalService._
        val server = ThriftMux.server.serveIface(
          new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
          new ExceptionalService[Future] {
            def deliver(input: String) = Future.value(input.length)
            def remove(id: Int): Future[Unit] = Future.Done
          }
        )

        val exceptionalServiceIface: ExceptionalService.ReqRepServicePerEndpoint =
          ThriftMux.client.servicePerEndpoint[ExceptionalService.ReqRepServicePerEndpoint](
            Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
            "simple"
          )

        val doubleFilter = new SimpleFilter[Request[Deliver.Args], Response[Deliver.SuccessType]] {
          def apply(
            request: Request[Deliver.Args],
            service: finagleService[Request[Deliver.Args], Response[Deliver.SuccessType]]
          ) =
            service(Request(request.args.copy(where = request.args.where + request.args.where)))
        }

        val filteredServiceIface =
          exceptionalServiceIface.withDeliver(
            deliver = doubleFilter.andThen(exceptionalServiceIface.deliver)
          )
        val methodIface = ThriftMux.Client.methodPerEndpoint(filteredServiceIface)
        Await.result(methodIface.deliver("123")) must be(6)

        Await.result(server.close(), 2.seconds)
      }

      "work with retrying filters" in { _ =>
        import com.twitter.finagle.service.{RetryExceptionsFilter, RetryPolicy}
        import com.twitter.util.{JavaTimer, Throw, Try}

        val service = serveExceptionalService()
        val clientService =
          ThriftMux.client.servicePerEndpoint[ExceptionalService.ReqRepServicePerEndpoint](
            Name.bound(Address(service.boundAddress.asInstanceOf[InetSocketAddress])),
            "client"
          )

        val retryPolicy =
          RetryPolicy.tries[Try[Int]](3, {
            case Throw(ex) if ex.getMessage == "Try again" =>
              true
          })

        val retriedDeliveryService =
          new RetryExceptionsFilter(retryPolicy, new JavaTimer(true)) andThen
            clientService.deliver
        Await.result(retriedDeliveryService(Request(Deliver.Args("there")))).value must be(123)

        Await.result(service.close(), 2.seconds)
      }

      "work with a methodPerEndpoint" in { _ =>
        val service = serveExceptionalService()
        val clientService =
          ThriftMux.client.servicePerEndpoint[ExceptionalService.ReqRepServicePerEndpoint](
            Name.bound(Address(service.boundAddress.asInstanceOf[InetSocketAddress])),
            "client"
          )

        val futureIface = ThriftMux.Client.methodPerEndpoint(clientService)

        intercept[EmptyXception] {
          Await.result(futureIface.deliver(""))
        }

        Await.result(service.close(), 2.seconds)
      }

      "work with both camelCase and snake_case function names" in { _ =>
        CamelCaseSnakeCaseService.FooBar.name mustBe "foo_bar"
        CamelCaseSnakeCaseService.BazQux.name mustBe "bazQux"

        val server = ThriftMux.server.serveIface(
          new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
          new CamelCaseSnakeCaseService[Future] {
            def fooBar(fooBar: String): Future[String] = Future.value(fooBar)
            def bazQux(bazQux: String): Future[String] = Future.value(bazQux)
          }
        )

        val client =
          ThriftMux.client.servicePerEndpoint[CamelCaseSnakeCaseService.ReqRepServicePerEndpoint](
            Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
            "client"
          )
        val richClient = ThriftMux.Client.methodPerEndpoint(client)

        Await.result(richClient.fooBar("foo")) mustBe "foo"
        Await.result(richClient.bazQux("baz")) mustBe "baz"

        Await.result(server.close(), 2.seconds)
      }

      "have correct stats" in { _ =>
        val service = serveExceptionalService()
        val statsReceiver = new InMemoryStatsReceiver
        val clientService = ThriftMux.client.withPerEndpointStats
          .configured(Stats(statsReceiver))
          .servicePerEndpoint[ExceptionalService.ReqRepServicePerEndpoint](
            Name.bound(Address(service.boundAddress.asInstanceOf[InetSocketAddress])),
            "customServiceName"
          )

        val futureIface = ThriftMux.Client.methodPerEndpoint(clientService)

        intercept[Xception] {
          Await.result(futureIface.deliver(where = "abc"))
        }

        eventually {
          statsReceiver.counters(
            Seq("customServiceName", "ExceptionalService", "deliver", "requests")
          ) must be(1)
        }
        eventually {
          statsReceiver.counters(
            Seq("customServiceName", "ExceptionalService", "deliver", "failures")
          ) must be(1)
        }
        eventually {
          statsReceiver.counters(
            Seq(
              "customServiceName",
              "ExceptionalService",
              "deliver",
              "failures",
              "thrift.test.Xception"
            )
          ) must be(1)
        }

        intercept[Xception] {
          Await.result(futureIface.deliver(where = "abc"))
        }

        // The 3rd request succeeds
        Await.result(futureIface.deliver(where = "abc"))

        eventually {
          statsReceiver.counters(
            Seq("customServiceName", "ExceptionalService", "deliver", "requests")
          ) must be(3)
        }
        eventually {
          statsReceiver.counters(
            Seq("customServiceName", "ExceptionalService", "deliver", "success")
          ) must be(1)
        }
        eventually {
          statsReceiver.counters(
            Seq("customServiceName", "ExceptionalService", "deliver", "failures")
          ) must be(2)
        }
        eventually {
          statsReceiver.counters(
            Seq(
              "customServiceName",
              "ExceptionalService",
              "deliver",
              "failures",
              "thrift.test.Xception"
            )
          ) must be(2)
        }

        Await.result(service.close(), 2.seconds)
      }

      "have stats with serviceName not set" in { _ =>
        val service = serveExceptionalService()
        val statsReceiver = new InMemoryStatsReceiver
        val clientService = ThriftMux.client.withPerEndpointStats
          .configured(Stats(statsReceiver))
          .servicePerEndpoint[ExceptionalService.ReqRepServicePerEndpoint](
            Name.bound(Address(service.boundAddress.asInstanceOf[InetSocketAddress])),
            "client"
          )

        val futureIface = ThriftMux.Client.methodPerEndpoint(clientService)

        intercept[Xception] {
          Await.result(futureIface.deliver(where = "abc"))
        }

        eventually {
          statsReceiver.counters(Seq("client", "ExceptionalService", "deliver", "requests")) must be(
            1
          )
        }

        Await.result(service.close(), 2.seconds)
      }
    }

    "generate eponymous Service" should {

      val context = new Mockery
      context.setImposteriser(ClassImposteriser.INSTANCE)
      val impl = context.mock(classOf[_root_.thrift.test.Service[Future]])
      val service = new _root_.thrift.test.Service$FinagleService(impl, RichServerParam())

      "allow generation and calls to eponymous FinagledService" in { _ =>
        context.checking(new Expectations {
          this.oneOf(impl).test(); will(returnValue(Future.value(())))
        })

        val request = encodeRequest("test", _root_.thrift.test.Service.Test.Args()).message
        val response = encodeResponse("test", _root_.thrift.test.Service.Test.Result())

        Await.result(service(request)) mustBe response

        context.assertIsSatisfied()
      }

      "allow generation and calls to eponymous FinagledClient" in { _ =>
        val clientService = new finagle.Service[ThriftClientRequest, Array[Byte]] {
          def apply(req: ThriftClientRequest) = service(req.message)
        }

        val client = new _root_.thrift.test.Service$FinagleClient(
          clientService,
          RichClientParam(serviceName = "Service")
        )

        context.checking(new Expectations {
          this.oneOf(impl).test(); will(returnValue(Future.Done))
        })

        Await.result(client.test()) mustBe ((): Unit)
        context.assertIsSatisfied()
      }
    }

    "generate exceptions with FailureFlags" in { _ =>
      val x = Xception(5, "hi")
      assert(x.flags == FailureFlags.Empty)

      val rejected = x.asRejected
      assert(rejected.flags == FailureFlags.Rejected)

      // verify flags are included in `equals` and `hashCode`
      assert(x != rejected)
      assert(x.hashCode != rejected.hashCode)
    }
  }
}
