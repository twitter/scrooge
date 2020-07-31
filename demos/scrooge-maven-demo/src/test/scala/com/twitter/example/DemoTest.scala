package com.twitter.example

import com.twitter.conversions.DurationOps._
import com.twitter.mydemo.renamed.User
import com.twitter.util.{Future, Await}
import java.net.{InetSocketAddress, SocketAddress}
import org.scalatest.funsuite.AnyFunSuite

class DemoTest extends AnyFunSuite {
  def printUser(user: User): Unit = println("User %s, id %d".format(user.name, user.id))

  def await[A](f: Future[A]): A = Await.result(f, 5.seconds)

  def getPort(address: SocketAddress): Int =
    address.asInstanceOf[InetSocketAddress].getPort

  test("generated finagle client and server") {
    val server = DemoServer.buildServer()
    val client = DemoClient.buildClient(getPort(server.boundAddress))

    await(client.createUser("Steph")).name == "Steph"
    await(client.createUser("Klay")).name == "Klay"

    client.asClosable.close(5.seconds)
    server.close(5.seconds)
  }

}
