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

class ServiceGeneratorSpec extends Specification with EvalHelper with JMocker with ClassMocker {
  import AST._

  val gen = new ScalaGenerator
  gen.scalaNamespace = "awwYeah"

  val protocol = mock[TProtocol]

  "ScalaGenerator service" should {
    "generate a service interface" in {
      val service = Service("Delivery", None, Array(
        Function("deliver", TI32, Array(Field(1, "where", TString, None, Requiredness.Default)), false, Array())
      ))
      compile(gen(service))

      val impl = "class DeliveryImpl(n: Int) extends awwYeah.Delivery.Iface { def deliver(where: String) = n }"
      compile(impl)

      invoke("new DeliveryImpl(3).deliver(\"Boston\")") mustEqual 3
    }

    "generate a future-based service interface" in {
      val service = Service("Delivery", None, Array(
        Function("deliver", TI32, Array(Field(1, "where", TString, None, Requiredness.Default)), false, Array())
      ))
      compile(gen(service))

      val impl = "import com.twitter.util.Future\n" +
        "class DeliveryImpl(n: Int) extends awwYeah.Delivery.FutureIface { def deliver(where: String) = Future(n) }"
      compile(impl)

      invoke("new DeliveryImpl(3).deliver(\"Boston\")()") mustEqual 3
    }

    "generate structs for args and return value" in {
      val service = Service("Delivery", None, Array(
        Function("deliver", TI32, Array(Field(1, "where", TString, None, Requiredness.Default)), false, Array())
      ))

      compile(gen(service))

      val impl = "class DeliveryImpl(n: Int) extends awwYeah.Delivery.Iface { def deliver(where: String) = n }"
      compile(impl)

      expect {
        startRead(protocol, new TField("where", TType.STRING, 1))
        one(protocol).readString() willReturn "boston"
        endRead(protocol)
      }

      val decoder = eval.inPlace[(TProtocol => ThriftStruct)]("awwYeah.Delivery.deliver_args.decoder")
      val obj = decoder(protocol)
      obj.getClass.getMethod("where").invoke(obj) mustEqual "boston"

      expect {
        startWrite(protocol, new TField("where", TType.STRING, 1))
        one(protocol).writeString("atlanta")
        endWrite(protocol)
      }

      eval.inPlace[ThriftStruct]("awwYeah.Delivery.deliver_args(\"atlanta\")").write(protocol)

      expect {
        startRead(protocol, new TField("success", TType.I32, 0))
        one(protocol).readI32() willReturn 13
        endRead(protocol)
      }

      val decoder2 = eval.inPlace[(TProtocol => ThriftStruct)]("awwYeah.Delivery.deliver_result.decoder")
      val obj2 = decoder2(protocol)
      obj2.getClass.getMethod("success").invoke(obj2) mustEqual 13

      expect {
        startWrite(protocol, new TField("success", TType.I32, 0))
        one(protocol).writeI32(24)
        endWrite(protocol)
      }

      eval.inPlace[ThriftStruct]("awwYeah.Delivery.deliver_result(24)").write(protocol)
    }
  }
}

