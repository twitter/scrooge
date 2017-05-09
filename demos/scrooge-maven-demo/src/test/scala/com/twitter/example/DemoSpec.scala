package com.twitter.example

import com.twitter.conversions.time._
import com.twitter.mydemo.renamed.User
import com.twitter.util.{Future, Await}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{MustMatchers, WordSpec}

@RunWith(classOf[JUnitRunner])
class DemoSpec extends WordSpec with MustMatchers {
  def printUser(user: User) {println("User %s, id %d".format(user.name, user.id))}

  def await[A](f: Future[A]): A = Await.result(f, 5.seconds)

  "generated finagle service" should {
    "server and client" in {
      val server = DemoServer.buildServer()
      val client = DemoClient.buildClient(server.boundAddress)
      await(client.createUser("Tyrion")).name must be("Tyrion")
      await(client.createUser("Jon")).name must be("Jon")
    }
  }
}

