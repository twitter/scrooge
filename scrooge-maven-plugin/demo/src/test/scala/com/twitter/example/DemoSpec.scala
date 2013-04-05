package com.twitter.example

import org.specs.SpecificationWithJUnit
import com.twitter.mydemo.renamed.User

class DemoSpec extends SpecificationWithJUnit {
  def printUser(user: User) {println("User %s, id %d".format(user.name, user.id))}

  "generated finagle service" should {
    "server and client" in {
      val server = DemoServer.buildServer()
      val client = DemoClient.buildClient(server.localAddress)
      client.createUser("Tyrion")().name mustEqual("Tyrion")
      client.createUser("Jon")().name mustEqual("Jon")
    }
  }
}

