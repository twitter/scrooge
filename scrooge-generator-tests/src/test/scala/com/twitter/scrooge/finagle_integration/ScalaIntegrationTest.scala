package com.twitter.scrooge.finagle_integration

import com.twitter.conversions.time._
import com.twitter.finagle._
import com.twitter.scrooge.{Request, Response, ThriftMethod}
import com.twitter.scrooge.finagle_integration.thriftscala.{BarService, ExtendedBarService}
import com.twitter.scrooge.finagle_integration.thriftscala.BarService.Echo
import com.twitter.scrooge.finagle_integration.thriftscala.ExtendedBarService.Triple
import com.twitter.util.{Await, Awaitable, Duration, Future}
import java.net.{InetAddress, InetSocketAddress}
import org.apache.thrift.TApplicationException
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

  def thriftBarClient(server: ListeningServer) = Thrift.client.build[BarService.MethodPerEndpoint] (
    Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
    "thriftBarClient"
  )

  def thriftExtendedBarClient(server: ListeningServer) = Thrift.client.build[ExtendedBarService.MethodPerEndpoint] (
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

  test("construct Thrift server with FutureIface -- backward compatible") {
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

  test("Fail to construct a Thrift server from Map[ThiriftMethod, Service[_,_]] if implementations are not found") {
    def mkPair(m: ThriftMethod)(f: m.Args => Future[m.SuccessType]): (ThriftMethod, Service[Request[_], Response[_]]) = {
      val reqRep = { r: Request[m.Args] =>
        f(r.args).map { x: m.SuccessType => Response(x) }
      }.asInstanceOf[m.ReqRepFunctionType]
      m -> m.toReqRepServicePerEndpointService(reqRep).asInstanceOf[Service[Request[_], Response[_]]]
     }

     val methods: Map[ThriftMethod, Service[Request[_], Response[_]]] = Map(
       mkPair(ExtendedBarService.Triple){ a: ExtendedBarService.Triple.Args => Future.value(a.z + a.z + a.z) },
       mkPair(BarService.Echo){ a: BarService.Echo.Args => Future.value(a.x) },
       mkPair(BarService.Duplicate){ a: BarService.Duplicate.Args => Future.value(a.y + a.y)},
       mkPair(BarService.SetDuck){ a: BarService.SetDuck.Args => Future.Unit }
       // Missing GetDuck
     )

    intercept[IllegalArgumentException] {
      ExtendedBarService.unsafeBuildFromMethods(methods)
    }
  }

  test("construct a Thrift server from Map[ThiriftMethod, Service[Request[_],Response[_]]") {
    def mkPair(m: ThriftMethod)(f: m.Args => Future[m.SuccessType]): (ThriftMethod, Service[Request[_], Response[_]]) = {
      val reqRep = { r: Request[m.Args] =>
        f(r.args).map { x: m.SuccessType => Response(x) }
      }.asInstanceOf[m.ReqRepFunctionType]
      m -> m.toReqRepServicePerEndpointService(reqRep).asInstanceOf[Service[Request[_], Response[_]]]
     }

     val methods: Map[ThriftMethod, Service[Request[_], Response[_]]] = Map(
       mkPair(ExtendedBarService.Triple){ a: ExtendedBarService.Triple.Args => Future.value(a.z + a.z + a.z) },
       mkPair(BarService.Echo){ a: BarService.Echo.Args => Future.value(a.x) },
       mkPair(BarService.Duplicate){ a: BarService.Duplicate.Args => Future.value(a.y + a.y)},
       mkPair(BarService.SetDuck){ a: BarService.SetDuck.Args => Future.Unit },
       mkPair(BarService.GetDuck){ a: BarService.GetDuck.Args => Future.value("Scrooge") }
     )

     val extendedBarService = Thrift.server.serveIface(
       new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
       ExtendedBarService.unsafeBuildFromMethods(methods).toThriftService
     )

     val clnt = thriftExtendedBarClient(extendedBarService)

     assert(await(clnt.echo("echo")) == "echo")
     assert(await(clnt.duplicate("y")) == "yy")
     assert(await(clnt.getDuck(3)) == "Scrooge")
     assert(await(clnt.setDuck(3, "x")) === ())
     assert(await(clnt.triple("x")) == "xxx")
   }

 test("Fail with a TApplicationException at request time if the implementation provided is incorrect") {
  // NB: As of now, the method to build a service from methods is marked "unsafe", but it would be
  // ideal if types could be checked on construction.

   def mkPair(m: ThriftMethod)(f: m.Args => Future[m.SuccessType]): (ThriftMethod, Service[Request[_], Response[_]]) = {
     val reqRep = { r: Request[m.Args] =>
       f(r.args).map { x: m.SuccessType => Response(x) }
     }.asInstanceOf[m.ReqRepFunctionType]
     m -> m.toReqRepServicePerEndpointService(reqRep).asInstanceOf[Service[Request[_], Response[_]]]
    }

    val echoService = new Service[Request[BarService.Echo.Args], Response[BarService.Echo.SuccessType]] {
     def apply(req: Request[BarService.Echo.Args]): Future[Response[BarService.Echo.SuccessType]] = {
       Future.value(Response(req.args.x))
     }
    }.asInstanceOf[Service[Request[_], Response[_]]]

    val methods: Map[ThriftMethod, Service[Request[_], Response[_]]] = Map(
      mkPair(ExtendedBarService.Triple){ a: ExtendedBarService.Triple.Args => Future.value(a.z + a.z + a.z) },
      mkPair(BarService.Echo){ a: BarService.Echo.Args => Future.value(a.x) },
      mkPair(BarService.Duplicate){ a: BarService.Duplicate.Args => Future.value(a.y + a.y)},
      mkPair(BarService.SetDuck){ a: BarService.SetDuck.Args => Future.Unit },
      BarService.GetDuck -> echoService
    )

    val extendedBarService = Thrift.server.serveIface(
      new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
      ExtendedBarService.unsafeBuildFromMethods(methods).toThriftService
    )

    val clnt = thriftExtendedBarClient(extendedBarService)

    intercept[TApplicationException] {
      await(clnt.getDuck(3))
    }

  }

  test("construct Thrift client with newIface[FutureIface] -- backward compatible") {
    val futureIfaceBarClient = Thrift.client.newIface[BarService.FutureIface] (
      Name.bound(Address(thriftBarServer.boundAddress.asInstanceOf[InetSocketAddress])),
      "futureIfaceBarClient"
    )

    val futureIfaceExtendedBarClient = Thrift.client.newIface[ExtendedBarService.FutureIface] (
      Name.bound(Address(thriftExtendedBarServer.boundAddress.asInstanceOf[InetSocketAddress])),
      "futureIfaceExtendedBarClient"
    )

    assert(await(futureIfaceBarClient.echo("echo")) == "echo")
    assert(await(futureIfaceExtendedBarClient.triple("3")) == "333")

  }

  test("construct Thrift client with Service[Future]]") {
    val serviceBarClient = Thrift.client.build[BarService[Future]] (
      Name.bound(Address(thriftBarServer.boundAddress.asInstanceOf[InetSocketAddress])),
      "serviceBarClient"
    )

    val serviceExtendedBarClient = Thrift.client.build[ExtendedBarService[Future]] (
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

  test("construct Thrift client with newServiceIface[ServiceIface] -- backward compatible") {
    val clientExtendedBarService = Thrift.client.newServiceIface[ExtendedBarService.ServiceIface](
      Name.bound(Address(thriftExtendedBarServer.boundAddress.asInstanceOf[InetSocketAddress])),
      "clientExtendedBarService"
    )

    val filteredServicePerEndpointCopy = clientExtendedBarService
      .copy(echo = echoFilter.andThen(clientExtendedBarService.echo))
      .copy(triple = tripleFilter.andThen(clientExtendedBarService.triple))

    val barMethodPerEndpointCopy = Thrift.Client.newMethodIface(filteredServicePerEndpointCopy)

    assert(await(barMethodPerEndpointCopy.echo("echo")) == "echoecho")
    assert(await(barMethodPerEndpointCopy.triple("3")) == "3.3.3.")
  }

  test("construct Thrift client with servicePerEndpoint[ServicePerEndpoint]") {
    val clientExtendedBarService = Thrift.client.servicePerEndpoint[ExtendedBarService.ServicePerEndpoint](
      Name.bound(Address(thriftExtendedBarServer.boundAddress.asInstanceOf[InetSocketAddress])),
      "clientExtendedBarService"
    )

    val filteredServicePerEndpointWith = clientExtendedBarService
      .withEcho(echo = echoFilter.andThen(clientExtendedBarService.echo))
      .withTriple(triple = tripleFilter.andThen(clientExtendedBarService.triple))

    val barMethodPerEndpointWith = Thrift.Client.methodPerEndpoint(filteredServicePerEndpointWith)

    assert(await(barMethodPerEndpointWith.echo("echo")) == "echoecho")
    assert(await(barMethodPerEndpointWith.triple("3")) == "3.3.3.")
  }
}
