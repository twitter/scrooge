package com.twitter.example

import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import com.twitter.mydemo.renamed.User

@RunWith(classOf[JUnitRunner])
class DemoSpec extends WordSpec with MustMatchers {
  def printUser(user: User) {println("User %s, id %d".format(user.name, user.id))}

  "generated finagle service" should {
    "server and client" in {
      val server = DemoServer.buildServer()
      val client = DemoClient.buildClient(server.localAddress)
      client.createUser("Tyrion")().name must be("Tyrion")
      client.createUser("Jon")().name must be("Jon")
    }
  }
}

