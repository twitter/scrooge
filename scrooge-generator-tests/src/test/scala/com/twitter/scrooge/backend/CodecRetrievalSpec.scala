package com.twitter.scrooge.backend

import com.twitter.scrooge._
import com.twitter.scrooge.testutil.EvalHelper
import com.twitter.scrooge.testutil.JMockSpec
import org.apache.thrift.protocol.TProtocol

class CodecRetrievalSpec extends JMockSpec with EvalHelper {
  "CodecRetrievalSpec" should {
    "retrieve codec for ordinary struct" in { _ =>
      ThriftStructCodec.forStructClass(classOf[includes.a.thriftscala.Address]) must be(
        includes.a.thriftscala.Address)
    }

    "retrieve codec for immutable struct" in { _ =>
      ThriftStructCodec.forStructClass(classOf[includes.a.thriftscala.Address.Immutable]) must be(
        includes.a.thriftscala.Address.Immutable)
    }

    "retrieve codec for union trait" in { _ =>
      ThriftStructCodec.forStructClass(classOf[includes.a.thriftscala.City]) must be(
        includes.a.thriftscala.City)
    }

    "retrieve codec for union case class" in { _ =>
      ThriftStructCodec.forStructClass(classOf[includes.a.thriftscala.City.CityState]) must be(
        includes.a.thriftscala.City)
    }

    "fail to retrieve codec for a class with no companion" in { _ =>
      val c = (new ThriftStruct {
        override def write(oprot: TProtocol): Unit = ???
      }).getClass
      assertThrows[ClassNotFoundException] {
        ThriftStructCodec.forStructClass(c)
      }
    }
  }
}
