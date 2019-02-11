package com.twitter.scrooge.backend

import com.twitter.conversions.DurationOps._
import com.twitter.finagle.param.Stats
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finagle.{Service => finagleService, _}
import com.twitter.scrooge.testutil.{EvalHelper, JMockSpec}
import com.twitter.util.{Await, Future}
import java.net.{InetAddress, InetSocketAddress}
import org.scalatest.concurrent.Eventually
import _root_.thrift.test.ExceptionalService._
import _root_.thrift.test._

// deprecated just so that it doesn't trigger deprecation warnings
@deprecated("", "")
class LegacyServiceGeneratorSpec extends JMockSpec with EvalHelper with Eventually {
  "ScalaGenerator service" should {

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

    "correctly inherit traits across services" should {

      "future-based" in { _ =>
        class FutureImpl extends ReadWriteService.FutureIface {
          def getName() = Future("Rus")
          def setName(name: String) = Future.Unit
        }

        new FutureImpl().isInstanceOf[ReadOnlyService.FutureIface] must be(true)
        new FutureImpl().isInstanceOf[ReadWriteService.FutureIface] must be(true)
      }
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

        val simpleService: SimpleService.ServiceIface =
          Thrift.client.newServiceIface[SimpleService.ServiceIface](
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

        val readOnlyClientService = Thrift.client.newServiceIface[ReadOnlyService.ServiceIface](
          Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
          "read-only"
        )
        Await.result(readOnlyClientService.getName(GetName.Args()), 5.seconds) must be(
          "Initial name"
        )

        val readWriteClientService = Thrift.client.newServiceIface[ReadWriteService.ServiceIface](
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

        val clientService = Thrift.client.newServiceIface[ExceptionalService.ServiceIface](
          Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
          "client"
        )

        intercept[EmptyXception] {
          Await.result(clientService.deliver(Deliver.Args("")), 5.seconds)
        }

        Await.result(server.close(), 2.seconds)
      }

      "work with filters on args" in { _ =>
        import SimpleService._
        val server = Thrift.server.serveIface(
          new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
          new SimpleService[Future] {
            def deliver(input: String) = Future.value(input.length)
          }
        )

        val simpleServiceIface: SimpleService.ServiceIface =
          Thrift.client.newServiceIface[SimpleService.ServiceIface](
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
          simpleServiceIface.copy(deliver = doubleFilter andThen simpleServiceIface.deliver)
        val methodIface = Thrift.Client.newMethodIface(filteredServiceIface)
        Await.result(methodIface.deliver("123")) must be(6)

        Await.result(server.close(), 2.seconds)
      }

      "work with retrying filters" in { _ =>
        import com.twitter.finagle.service.{RetryExceptionsFilter, RetryPolicy}
        import com.twitter.util.{JavaTimer, Throw, Try}

        val service = serveExceptionalService()
        val clientService = Thrift.client.newServiceIface[ExceptionalService.ServiceIface](
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

      "work with a newMethodIface" in { _ =>
        val service = serveExceptionalService()
        val clientService = Thrift.client.newServiceIface[ExceptionalService.ServiceIface](
          Name.bound(Address(service.boundAddress.asInstanceOf[InetSocketAddress])),
          "client"
        )

        val futureIface = Thrift.Client.newMethodIface(clientService)

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

        val client = Thrift.client.newServiceIface[CamelCaseSnakeCaseService.ServiceIface](
          Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
          "client"
        )
        val richClient = Thrift.Client.newMethodIface(client)

        Await.result(richClient.fooBar("foo")) mustBe "foo"
        Await.result(richClient.bazQux("baz")) mustBe "baz"

        Await.result(server.close(), 2.seconds)
      }

      "have correct stats" in { _ =>
        val service = serveExceptionalService()
        val statsReceiver = new InMemoryStatsReceiver
        val clientService = Thrift.client.withPerEndpointStats
          .configured(Stats(statsReceiver))
          .newServiceIface[ExceptionalService.ServiceIface](
            Name.bound(Address(service.boundAddress.asInstanceOf[InetSocketAddress])),
            "customServiceName"
          )

        val futureIface = Thrift.Client.newMethodIface(clientService)

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
        val clientService = Thrift.client
          .configured(Stats(statsReceiver))
          .withPerEndpointStats
          .newServiceIface[ExceptionalService.ServiceIface](
            Name.bound(Address(service.boundAddress.asInstanceOf[InetSocketAddress])),
            "client"
          )

        val futureIface = Thrift.Client.newMethodIface(clientService)

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
  }
}
