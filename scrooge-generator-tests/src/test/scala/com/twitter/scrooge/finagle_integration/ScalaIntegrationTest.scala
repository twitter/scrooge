package com.twitter.scrooge.finagle_integration

import com.twitter.conversions.time._
import com.twitter.finagle.{Address, Name, Thrift, ThriftMux}
import com.twitter.scrooge.finagle_integration.thriftscala.BarService
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

  val thriftServer = Thrift.server.serveIface(
    new InetSocketAddress(InetAddress.getLoopbackAddress, 0),
    iface
  )

  val thriftClient = Thrift.client.newIface[BarService.FutureIface](
    Name.bound(Address(thriftServer.boundAddress.asInstanceOf[InetSocketAddress])),
    "client"
  )

  test("Thrift client should be able to call Thrift server") {
    assert(await(thriftClient.echo("hello")) == "hello")
    assert(await(thriftClient.duplicate("hi")) == "hihi")
    assert(await(thriftClient.getDuck(10L)) == "Scrooge")
    assert(await(thriftClient.setDuck(20L, "McDuck")) === ())
    await(thriftServer.close())
  }

}
