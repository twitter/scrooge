package com.twitter.example

import com.twitter.finagle.Thrift
import com.twitter.finagle.builder.{ServerBuilder, ClientBuilder}
import com.twitter.mydemo.renamed.{User, UserService}
import com.twitter.util.Future
import java.net.{SocketAddress, InetSocketAddress}
import java.util.concurrent.atomic.AtomicInteger
import org.apache.thrift.protocol.TBinaryProtocol
import com.twitter.finagle.ListeningServer

object DemoClient {
  def buildClient(address: SocketAddress): UserService.FinagledClient = {
    val clientService = ClientBuilder()
      .hosts(address.asInstanceOf[InetSocketAddress])
      .stack(Thrift.client)
      .hostConnectionLimit(1)
      .build()
    new UserService.FinagledClient(clientService)
  }
}

object DemoServer {
  private val userIdCounter: AtomicInteger = new AtomicInteger(0)

  // need to provide an implementation for the finagle service
  class MyUserImpl extends UserService.FutureIface {
    def createUser(name: String): Future[User] = {
      val id = userIdCounter.incrementAndGet()
      Future.value(User(id, name))
    }
  }

  def buildServer(): ListeningServer = {
    val protocol = new TBinaryProtocol.Factory()
    val serverService = new UserService.FinagledService(new MyUserImpl, protocol)
    ServerBuilder()
      .stack(Thrift.server)
      .name("binary_service")
      .bindTo(new InetSocketAddress(0))
      .build(serverService)
  }
}
