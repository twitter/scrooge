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

  type ThriftStruct = { def write(oprot: TProtocol) }

  val gen = new ScalaGenerator
  val namespace = Namespace("scala", "awwYeah")
  val doc = Document(Seq(namespace), Nil)

  val protocol = mock[TProtocol]

  "ScalaGenerator service" should {
    "generate a service interface" in {
      val service = Service("Delivery", None, Seq(
        Function("deliver", TI32, Seq(Field(1, "where", TString)), false, Seq())
      ))
      compile(gen(doc, service))

      val impl = "class DeliveryImpl(n: Int) extends awwYeah.Delivery.Iface { def deliver(where: String) = n }"
      compile(impl)

      invoke("new DeliveryImpl(3).deliver(\"Boston\")") mustEqual 3
    }

    "generate a future-based service interface" in {
      val service = Service("Delivery", None, Seq(
        Function("deliver", TI32, Seq(Field(1, "where", TString)), false, Seq())
      ))
      compile(gen(doc, service))

      val impl = "import com.twitter.util.Future\n" +
        "class DeliveryImpl(n: Int) extends awwYeah.Delivery.FutureIface { def deliver(where: String) = Future(n) }"
      compile(impl)

      invoke("new DeliveryImpl(3).deliver(\"Boston\")()") mustEqual 3
    }

    "generate structs for args and return value" in {
      val service = Service("Delivery", None, Seq(
        Function("deliver", TI32, Seq(Field(1, "where", TString)), false, Seq())
      ))

      compile(gen(doc, service))

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
      obj2.getClass.getMethod("success").invoke(obj2) mustEqual Some(13)

      expect {
        startWrite(protocol, new TField("success", TType.I32, 0))
        one(protocol).writeI32(24)
        endWrite(protocol)
      }

      eval.inPlace[ThriftStruct]("awwYeah.Delivery.deliver_result(Some(24))").write(protocol)
    }

    "generate exception return values" in {
      val exception1 = Exception_("Error", Seq(Field(1, "description", TString)))

      compile(gen(doc, exception1))

      val service = Service("Delivery", None, Seq(
        Function("deliver", TI32, Seq(
          Field(1, "where", TString)
        ), false, Seq(
          Field(3, "ex1", StructType(exception1))
        ))
      ))

      compile(gen(doc, service))

      expect {
        startRead(protocol, new TField("ex1", TType.STRUCT, 3))
        startRead(protocol, new TField("description", TType.STRING, 1))
        one(protocol).readString() willReturn "silly"
        endRead(protocol)
        endRead(protocol)
      }

      val decoder = eval.inPlace[(TProtocol => ThriftStruct)]("awwYeah.Delivery.deliver_result.decoder")
      val obj = decoder(protocol)
      val optEx1 = obj.getClass.getMethod("ex1").invoke(obj)
      optEx1 must beLike {
        case Some(ex1: AnyRef) =>
          ex1.getClass.getMethod("description").invoke(ex1) mustEqual "silly"
          true
      }

      expect {
        startWrite(protocol, new TField("success", TType.I32, 0))
        one(protocol).writeI32(24)
        endWrite(protocol)
      }

      eval.inPlace[ThriftStruct]("awwYeah.Delivery.deliver_result(Some(24), None)").write(protocol)

      expect {
        startWrite(protocol, new TField("ex1", TType.STRUCT, 3))
        startWrite(protocol, new TField("description", TType.STRING, 1))
        one(protocol).writeString("silly")
        endWrite(protocol)
        endWrite(protocol)
      }

      eval.inPlace[ThriftStruct]("awwYeah.Delivery.deliver_result(None, Some(new awwYeah.Error(\"silly\")))").write(protocol)
    }

    "generate service and client" in {
      val ex = Exception_("Boom", Nil)
      val exs = Seq(Field(1, "ex", StructType(ex)))
      val service = Service("Delivery", None, Seq(
        Function("deliver", TI32, Seq(Field(1, "where", TString)), false, Nil),
//        Function("deliver2", TI32, Seq(Field(1, "where", TString)), false, exs) // blows-up, why?
        Function("execute", Void, Nil, false, Nil)
//        Function("execute2", Void, Nil, false, exs) // blows-up, why?
      ))
      val doc = Document(Nil, Seq(ex, service))
      val genOptions = Set[ScalaServiceOption](WithFinagleClient, WithFinagleService, WithOstrichServer)
      compile(gen(doc, genOptions)) //must not(throwA[Exception])
    }
  }
}

