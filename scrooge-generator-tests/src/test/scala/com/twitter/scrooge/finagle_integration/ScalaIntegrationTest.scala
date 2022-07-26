package com.twitter.scrooge.finagle_integration

import com.twitter.conversions.DurationOps._
import com.twitter.finagle._
import com.twitter.scrooge.Request
import com.twitter.scrooge.Response
import com.twitter.scrooge.ThriftMethod
import com.twitter.scrooge.finagle_integration.thriftscala._
import com.twitter.scrooge.finagle_integration.thriftscala.BarService.Echo
import com.twitter.scrooge.finagle_integration.thriftscala.ExtendedBarService.Triple
import com.twitter.util.Await
import com.twitter.util.Awaitable
import com.twitter.util.Duration
import com.twitter.util.Future
import java.net.InetAddress
import java.net.InetSocketAddress
import org.apache.thrift.TApplicationException
import org.scalatest.funsuite.AnyFunSuite

class ScalaIntegrationTest extends AnyFunSuite {

  def await[T](a: Awaitable[T], d: Duration = 5.seconds): T =
    Await.result(a, d)

  val iface = new BarService.MethodPerEndpoint {
    override def echo(x: String): Future[String] = Future.value(x)

    override def duplicate(y: String): Future[String] = Future.value(y + y)

    override def getDuck(key: Long): Future[String] = Future.value("Scrooge")

    override def setDuck(key: Long, value: String): Future[Unit] = Future.Unit

    override def regression(arg: Option[scala.collection.Set[String]]): Future[RegressionStruct] =
      Future.value(RegressionStruct(List("regression")))
  }

  val muxServer =
    ThriftMux.server.serveIface(new InetSocketAddress(InetAddress.getLoopbackAddress, 0), iface)

  val muxClient = ThriftMux.client.build[BarService.MethodPerEndpoint](
    Name.bound(Address(muxServer.boundAddress.asInstanceOf[InetSocketAddress])),
    "client"
  )

  test("ThriftMux client should be able to call ThriftMux server") {
    assert(await(muxClient.echo("hello")) == "hello")
    assert(await(muxClient.duplicate("hi")) == "hihi")
    assert(await(muxClient.getDuck(10L)) == "Scrooge")
    assert(await(muxClient.setDuck(20L, "McDuck")) === (()))
    await(muxServer.close())
  }

  def thriftBarClient(server: ListeningServer) = Thrift.client.build[BarService.MethodPerEndpoint](
    Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
    "thriftBarClient"
  )

  def thriftExtendedBarClient(server: ListeningServer) =
    Thrift.client.build[ExtendedBarService.MethodPerEndpoint](
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

      def regression(arg: Option[scala.collection.Set[String]]): Future[RegressionStruct] =
        Future.value(RegressionStruct(List("regression")))
    }
  )

  val thriftExtendedBarServer = Thrift.server.serveIface(
    new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
    new ExtendedBarService.MethodPerEndpoint {
      override def echo(x: String): Future[String] = Future.value(x)

      override def duplicate(y: String): Future[String] = Future.value(y + y)

      override def getDuck(key: Long): Future[String] = Future.value("Scrooge")

      override def setDuck(key: Long, value: String): Future[Unit] = Future.Unit

      override def triple(z: String): Future[String] = Future.value(z + z + z)

      def regression(arg: Option[scala.collection.Set[String]]): Future[RegressionStruct] =
        Future.value(RegressionStruct(List("regression")))
    }
  )

  test("construct Thrift server with MethodPerEndpoint") {
    assert(await(thriftBarClient(thriftBarServer).echo("echo")) == "echo")
    assert(await(thriftExtendedBarClient(thriftExtendedBarServer).triple("3")) == "333")
  }

  test(
    "Fail to construct a Thrift server from Map[ThiriftMethod, Service[_,_]] if implementations are not found"
  ) {
    def mkPair(
      m: ThriftMethod
    )(
      f: m.Args => Future[m.SuccessType]
    ): (ThriftMethod, Service[Request[_], Response[_]]) = {
      val reqRep = { r: Request[m.Args] => f(r.args).map { x: m.SuccessType => Response(x) } }
        .asInstanceOf[m.ReqRepFunctionType]
      m -> m
        .toReqRepServicePerEndpointService(reqRep).asInstanceOf[Service[Request[_], Response[_]]]
    }

    val methods: Map[ThriftMethod, Service[Request[_], Response[_]]] = Map(
      mkPair(ExtendedBarService.Triple) { a: ExtendedBarService.Triple.Args =>
        Future.value(a.z + a.z + a.z)
      },
      mkPair(BarService.Echo) { a: BarService.Echo.Args => Future.value(a.x) },
      mkPair(BarService.Duplicate) { a: BarService.Duplicate.Args => Future.value(a.y + a.y) },
      mkPair(BarService.SetDuck) { a: BarService.SetDuck.Args => Future.Unit },
      mkPair(BarService.Regression) { a: BarService.Regression.Args =>
        Future.value(RegressionStruct(List("regression")))
      }
      // Missing GetDuck
    )

    intercept[IllegalArgumentException] {
      ExtendedBarService.unsafeBuildFromMethods(methods)
    }
  }

  test("construct a Thrift server from Map[ThiriftMethod, Service[Request[_],Response[_]]") {
    def mkPair(
      m: ThriftMethod
    )(
      f: m.Args => Future[m.SuccessType]
    ): (ThriftMethod, Service[Request[_], Response[_]]) = {
      val reqRep = { r: Request[m.Args] => f(r.args).map { x: m.SuccessType => Response(x) } }
        .asInstanceOf[m.ReqRepFunctionType]
      m -> m
        .toReqRepServicePerEndpointService(reqRep).asInstanceOf[Service[Request[_], Response[_]]]
    }

    val methods: Map[ThriftMethod, Service[Request[_], Response[_]]] = Map(
      mkPair(ExtendedBarService.Triple) { a: ExtendedBarService.Triple.Args =>
        Future.value(a.z + a.z + a.z)
      },
      mkPair(BarService.Echo) { a: BarService.Echo.Args => Future.value(a.x) },
      mkPair(BarService.Duplicate) { a: BarService.Duplicate.Args => Future.value(a.y + a.y) },
      mkPair(BarService.SetDuck) { a: BarService.SetDuck.Args => Future.Unit },
      mkPair(BarService.GetDuck) { a: BarService.GetDuck.Args => Future.value("Scrooge") },
      mkPair(BarService.Regression) { a: BarService.Regression.Args =>
        Future.value(RegressionStruct(List("regression")))
      }
    )

    val extendedBarService = Thrift.server.serveIface(
      new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
      ExtendedBarService.unsafeBuildFromMethods(methods).toThriftService
    )

    val clnt = thriftExtendedBarClient(extendedBarService)

    assert(await(clnt.echo("echo")) == "echo")
    assert(await(clnt.duplicate("y")) == "yy")
    assert(await(clnt.getDuck(3)) == "Scrooge")
    assert(await(clnt.setDuck(3, "x")) === (()))
    assert(await(clnt.triple("x")) == "xxx")
  }

  test(
    "Fail with a TApplicationException at request time if the implementation provided is incorrect"
  ) {
    // NB: As of now, the method to build a service from methods is marked "unsafe", but it would be
    // ideal if types could be checked on construction.

    def mkPair(
      m: ThriftMethod
    )(
      f: m.Args => Future[m.SuccessType]
    ): (ThriftMethod, Service[Request[_], Response[_]]) = {
      val reqRep = { r: Request[m.Args] => f(r.args).map { x: m.SuccessType => Response(x) } }
        .asInstanceOf[m.ReqRepFunctionType]
      m -> m
        .toReqRepServicePerEndpointService(reqRep).asInstanceOf[Service[Request[_], Response[_]]]
    }

    val echoService =
      new Service[Request[BarService.Echo.Args], Response[BarService.Echo.SuccessType]] {
        def apply(
          req: Request[BarService.Echo.Args]
        ): Future[Response[BarService.Echo.SuccessType]] = {
          Future.value(Response(req.args.x))
        }
      }.asInstanceOf[Service[Request[_], Response[_]]]

    val methods: Map[ThriftMethod, Service[Request[_], Response[_]]] = Map(
      mkPair(ExtendedBarService.Triple) { a: ExtendedBarService.Triple.Args =>
        Future.value(a.z + a.z + a.z)
      },
      mkPair(BarService.Echo) { a: BarService.Echo.Args => Future.value(a.x) },
      mkPair(BarService.Duplicate) { a: BarService.Duplicate.Args => Future.value(a.y + a.y) },
      mkPair(BarService.SetDuck) { a: BarService.SetDuck.Args => Future.Unit },
      BarService.GetDuck -> echoService,
      mkPair(BarService.Regression) { a: BarService.Regression.Args =>
        Future.value(RegressionStruct(List("regression")))
      }
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

  val tripleFilter = new SimpleFilter[Triple.Args, Triple.SuccessType] {
    def apply(args: Triple.Args, service: Service[Triple.Args, Triple.SuccessType]) =
      service(args.copy(z = args.z + "."))
  }

  val echoFilter = new SimpleFilter[Echo.Args, Echo.SuccessType] {
    def apply(args: Echo.Args, service: Service[Echo.Args, Echo.SuccessType]) =
      service(args.copy(x = args.x + args.x))
  }

  test("construct Thrift client with servicePerEndpoint[ServicePerEndpoint]") {
    val server = Thrift.server.serveIface(
      new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
      new ExtendedBarService.MethodPerEndpoint {
        def echo(x: String): Future[String] = Future.value(x)

        def duplicate(y: String): Future[String] = Future.value(y + y)

        def getDuck(key: Long): Future[String] = Future.value("Scrooge")

        def setDuck(key: Long, value: String): Future[Unit] =
          Future.exception(new InvalidQueryException(value.length))

        def triple(z: String): Future[String] = Future.value(z + z + z)

        def regression(arg: Option[scala.collection.Set[String]]): Future[RegressionStruct] =
          Future.value(RegressionStruct(List("regression")))
      }
    )

    val clientExtendedBarService =
      Thrift.client.servicePerEndpoint[ExtendedBarService.ServicePerEndpoint](
        Name.bound(Address(server.boundAddress.asInstanceOf[InetSocketAddress])),
        "clientExtendedBarService"
      )

    val ex = intercept[InvalidQueryException] {
      await(clientExtendedBarService.setDuck(BarService.SetDuck.Args(1L, "hi")))
    }
    assert("hi".length == ex.errorCode)

    val filteredServicePerEndpointWith = clientExtendedBarService
      .withEcho(echo = echoFilter.andThen(clientExtendedBarService.echo))
      .withTriple(triple = tripleFilter.andThen(clientExtendedBarService.triple))

    val barMethodPerEndpointWith = Thrift.Client.methodPerEndpoint(filteredServicePerEndpointWith)

    val eex = intercept[InvalidQueryException] {
      await(barMethodPerEndpointWith.setDuck(1L, "hi"))
    }
    assert("hi".length == eex.errorCode)
    assert(await(barMethodPerEndpointWith.echo("echo")) == "echoecho")
    assert(await(barMethodPerEndpointWith.triple("3")) == "3.3.3.")
  }
}
