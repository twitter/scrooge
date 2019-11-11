package com.twitter.example

import com.twitter.finagle.{ListeningServer, Thrift}
import com.twitter.mydemo.renamed.{User, UserService}
import com.twitter.util.Future
import java.util.concurrent.atomic.AtomicInteger

object DemoClient {

  def buildClient(port: Int): UserService.MethodPerEndpoint =
    Thrift.client
      .withLabel("demo-client")
      .build[UserService.MethodPerEndpoint](s"localhost:$port")

}

object DemoServer {
  private val userIdCounter: AtomicInteger = new AtomicInteger(0)

  // need to provide an implementation for the finagle service
  class MyUserImpl extends UserService.MethodPerEndpoint {
    def createUser(name: String): Future[User] = {
      val id = userIdCounter.incrementAndGet()
      Future.value(User(id, name))
    }
  }

  def buildServer(): ListeningServer = {
    val userService = new MyUserImpl
    Thrift.server
      .withLabel("demo-server")
      .serveIface("localhost:0", userService)
  }

}
