package com.twitter.scrooge
package scalagen

import java.nio.ByteBuffer
import scala.collection.mutable
import scala.collection.JavaConversions._
import com.twitter.util.Eval
import org.specs.Specification
import org.specs.matcher.Matcher
import org.specs.mock.{ClassMocker, JMocker}
import org.apache.thrift.protocol._
import java.security.MessageDigest
import java.math.BigInteger

class ServiceGeneratorSpec extends Specification with EvalHelper with JMocker {
  import AST._

  val gen = new ScalaGenerator
  gen.scalaNamespace = "awwYeah"

  "ScalaGenerator service" should {
    "generate a service" in {
      val service = Service("Delivery", None, Array(
        Function("deliver", TI32, Array(Field(1, "where", TString, None, Requiredness.Default)), false, Array())
      ))
      compile(gen(service))

      val impl = "class DeliveryImpl(n: Int) extends awwYeah.Delivery.Iface { def deliver(where: String) = n }"
      compile(impl)

      invoke("new DeliveryImpl(3).deliver(\"Boston\")") mustEqual 3
    }
  }
}

