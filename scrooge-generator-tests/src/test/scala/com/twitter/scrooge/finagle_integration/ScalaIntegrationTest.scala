package com.twitter.scrooge.finagle_integration

import com.twitter.conversions.time._
import com.twitter.finagle._
import com.twitter.scrooge.finagle_integration.thriftscala.{BarService, ExtendedBarService}
import com.twitter.scrooge.finagle_integration.thriftscala.BarService.Echo
import com.twitter.scrooge.finagle_integration.thriftscala.ExtendedBarService.Triple
import com.twitter.util.{Await, Awaitable, Duration, Future}
import java.net.{InetAddress, InetSocketAddress}
import org.scalatest.FunSuite

class ScalaIntegrationTest extends FunSuite {

  def await[T](a: Awaitable[T], d: Duration = 5.seconds): T =
    Await.result(a, d)

  val iface = new BarService.FutureIface {
    override def echo(x: String): Future[String] = Future.value(x)

    override def duplicate(y: String): Future[String] = Future.value(y + y)

    override def getDuck(key: Long): Future[String] = Future.value("Scrooge")

    override def setDuck(key: Long, value: String): Future[Unit] = Future.Unit
  }

  val muxServer =
    ThriftMux.server.serveIface(new InetSocketAddress(InetAddress.getLoopbackAddress, 0), iface)

  val muxClient = ThriftMux.client.newIface[BarService.FutureIface](
    Name.bound(Address(muxServer.boundAddress.asInstanceOf[InetSocketAddress])),
    "client"
  )

  test("ThriftMux client should be able to call ThriftMux server") {
    assert(await(muxClient.echo("hello")) == "hello")
    assert(await(muxClient.duplicate("hi")) == "hihi")
    assert(await(muxClient.getDuck(10L)) == "Scrooge")
    assert(await(muxClient.setDuck(20L, "McDuck")) === ())
    await(muxServer.close())
  }

  def thriftBarClient(server: ListeningServer) = Thrift.client.newIface[BarService.MethodPerEndpoint] (
    Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
    "thriftBarClient"
  )

  def thriftExtendedBarClient(server: ListeningServer) = Thrift.client.newIface[ExtendedBarService.MethodPerEndpoint] (
    Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
    "thriftExtendedBarClient"
  )

  val thriftBarServer = Thrift.server.serveIface(
    new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
    new BarService.MethodPerEndpoint {
      def echo(x: String): Future[String] = Future.value(x)

      def duplicate(y: String): Future[String] = Future.value(y + y)

      def getDuck(key: Long): Future[String] = Future.value("Scrooge")

      def setDuck(key: Long, value: String): Future[Unit] = Future.Unit
    }
  )

  val thriftExtendedBarServer = Thrift.server.serveIface(
    new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
    new ExtendedBarService.MethodPerEndpoint {
      override def echo(x: String): Future[String] = Future.value(x)

      override def duplicate(y: String): Future[String] = Future.value(y + y)

      override def getDuck(key: Long): Future[String] =  Future.value("Scrooge")

      override def setDuck(key: Long, value: String): Future[Unit] = Future.Unit

      override def triple(z: String): Future[String] = Future.value(z + z + z)
    }
  )

  test("construct Thrift server with FutureIface") {
    val futureIfaceBarServer = Thrift.server.serveIface(
      new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
      new BarService.FutureIface {
        def echo(x: String): Future[String] = Future.value(x)

        def duplicate(y: String): Future[String] = Future.value(y + y)

        def getDuck(key: Long): Future[String] = Future.value("Scrooge")

        def setDuck(key: Long, value: String): Future[Unit] = Future.Unit
      }
    )

    val futureIfaceExtendedBarServer = Thrift.server.serveIface(
      new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
      new ExtendedBarService.FutureIface {
        def echo(x: String): Future[String] = Future.value(x)

        def duplicate(y: String): Future[String] = Future.value(y + y)

        def getDuck(key: Long): Future[String] = Future.value("Scrooge")

        def setDuck(key: Long, value: String): Future[Unit] = Future.Unit

        def triple(z: String): Future[String] = Future.value(z + z + z)
      }
    )

    assert(await(thriftBarClient(futureIfaceBarServer).echo("echo")) == "echo")
    assert(await(thriftExtendedBarClient(futureIfaceExtendedBarServer).triple("3")) == "333")

  }

  test("construct Thrift server with Service[Future]") {
    val serviceBarServer = Thrift.server.serveIface(
      new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
      new BarService[Future] {
        def echo(x: String): Future[String] = Future.value(x)

        def duplicate(y: String): Future[String] = Future.value(y + y)

        def getDuck(key: Long): Future[String] = Future.value("Scrooge")

        def setDuck(key: Long, value: String): Future[Unit] = Future.Unit
      }
    )

    val serviceExtendedBarServer = Thrift.server.serveIface(
      new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
      new ExtendedBarService[Future] {
        def echo(x: String): Future[String] = Future.value(x)

        def duplicate(y: String): Future[String] = Future.value(y + y)

        def getDuck(key: Long): Future[String] = Future.value("Scrooge")

        def setDuck(key: Long, value: String): Future[Unit] = Future.Unit

        def triple(z: String): Future[String] = Future.value(z + z + z)
      }
    )
    assert(await(thriftBarClient(serviceBarServer).echo("echo")) == "echo")
    assert(await(thriftExtendedBarClient(serviceExtendedBarServer).triple("3")) == "333")
  }

  test("construct Thrift server with MethodPerEndpoint") {
    assert(await(thriftBarClient(thriftBarServer).echo("echo")) == "echo")
    assert(await(thriftExtendedBarClient(thriftExtendedBarServer).triple("3")) == "333")
  }

  test("construct Thrift client with newface[FutureIface]") {
    val futureIfaceBarClient = Thrift.client.newIface[BarService.MethodPerEndpoint] (
      Name.bound(Address(thriftBarServer.boundAddress.asInstanceOf[InetSocketAddress])),
      "futureIfaceBarClient"
    )

    val futureIfaceExtendedBarClient = Thrift.client.newIface[ExtendedBarService.MethodPerEndpoint] (
      Name.bound(Address(thriftExtendedBarServer.boundAddress.asInstanceOf[InetSocketAddress])),
      "futureIfaceExtendedBarClient"
    )

    assert(await(futureIfaceBarClient.echo("echo")) == "echo")
    assert(await(futureIfaceExtendedBarClient.triple("3")) == "333")

  }

  test("construct Thrift client with Service[Future]]") {
    val serviceBarClient = Thrift.client.newIface[BarService[Future]] (
      Name.bound(Address(thriftBarServer.boundAddress.asInstanceOf[InetSocketAddress])),
      "serviceBarClient"
    )

    val serviceExtendedBarClient = Thrift.client.newIface[ExtendedBarService[Future]] (
      Name.bound(Address(thriftExtendedBarServer.boundAddress.asInstanceOf[InetSocketAddress])),
      "serviceExtendedBarClient"
    )

    assert(await(serviceBarClient.echo("echo")) == "echo")
    assert(await(serviceExtendedBarClient.triple("3")) == "333")
  }

  val tripleFilter = new SimpleFilter[Triple.Args, Triple.SuccessType] {
    def apply(args: Triple.Args, service: Service[Triple.Args, Triple.SuccessType]) =
      service(args.copy(z = args.z + "."))
  }

  val echoFilter = new SimpleFilter[Echo.Args, Echo.SuccessType] {
    def apply(args: Echo.Args, service: Service[Echo.Args, Echo.SuccessType]) =
      service(args.copy(x = args.x + args.x))
  }

  test("construct Thrift client with newServiceIface[ServiceIface]") {
    val clientExtendedBarService = Thrift.client.newServiceIface[ExtendedBarService.ServiceIface](
      Name.bound(Address(thriftExtendedBarServer.boundAddress.asInstanceOf[InetSocketAddress])),
      "clientExtendedBarService"
    )

    val filteredServicePerEndpointCopy = clientExtendedBarService
      .copy(echo = echoFilter.andThen(clientExtendedBarService.echo))
      .copy(triple = tripleFilter.andThen(clientExtendedBarService.triple))

    val barMethodPerEndpointCopy = Thrift.client.newMethodIface(filteredServicePerEndpointCopy)

    assert(await(barMethodPerEndpointCopy.echo("echo")) == "echoecho")
    assert(await(barMethodPerEndpointCopy.triple("3")) == "3.3.3.")
  }

  test("construct Thrift client with newServiceIface[ServicePerEndpoint]") {
    val clientExtendedBarService = Thrift.client.servicePerEndpoint[ExtendedBarService.ServicePerEndpoint](
      Name.bound(Address(thriftExtendedBarServer.boundAddress.asInstanceOf[InetSocketAddress])),
      "clientExtendedBarService"
    )

    val filteredServicePerEndpointWith = clientExtendedBarService
      .withEcho(echo = echoFilter.andThen(clientExtendedBarService.echo))
      .withTriple(triple = tripleFilter.andThen(clientExtendedBarService.triple))

    val barMethodPerEndpointWith = Thrift.client.methodPerEndpoint(filteredServicePerEndpointWith)

    assert(await(barMethodPerEndpointWith.echo("echo")) == "echoecho")
    assert(await(barMethodPerEndpointWith.triple("3")) == "3.3.3.")
  }

  test("serve multiple services") {
    import scala.language.reflectiveCalls

    class BarMethodPerEndpoint extends BarService.MethodPerEndpoint {
      override def echo(x: String): Future[String] = Future.value(x)

      override def duplicate(y: String): Future[String] = Future.value(y + y)

      override def getDuck(key: Long): Future[String] = Future.value("Scrooge")

      override def setDuck(key: Long, value: String): Future[Unit] = Future.Unit
    }

    class ExtendedBarMethodPerEndpoint extends ExtendedBarService.MethodPerEndpoint {
      override def echo(x: String): Future[String] = Future.value(x)

      override def duplicate(y: String): Future[String] = Future.value(y + y)

      override def getDuck(key: Long): Future[String] =  Future.value("Scrooge")

      override def setDuck(key: Long, value: String): Future[Unit] = Future.Unit

      override def triple(z: String): Future[String] = Future.value(z + z + z)
    }

    val serviceMap = Map(
      "bar" -> new BarMethodPerEndpoint(),
      "extendedBar" -> new ExtendedBarMethodPerEndpoint()
    )

    val address = new InetSocketAddress(InetAddress.getLoopbackAddress, 0)
    val server = Thrift.server.serveIfaces(address, serviceMap, Some("extendedBar"))

    val name = Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress]))
    val client = Thrift.client.multiplex(name, "client") { client =>
      new {
        val bar = client.newIface[BarService.MethodPerEndpoint]("bar")
        val extendedBar = client.servicePerEndpoint[ExtendedBarService.ServicePerEndpoint]("extendedBar")
      }
    }

    assert(await(client.bar.echo("hello")) == "hello")

    val triple = await(client.extendedBar.triple(ExtendedBarService.Triple.Args("3")))
    assert(triple == "333")

    val classicClient = Thrift.client.newIface[ExtendedBarService.MethodPerEndpoint](name, "classic-client")
    assert(await(classicClient.triple("3")) == "333")
  }
}
