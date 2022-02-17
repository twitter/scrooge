package com.twitter.scrooge.backend

import com.twitter.scrooge._
import com.twitter.scrooge.testutil.EvalHelper
import com.twitter.scrooge.testutil.JMockSpec
import org.apache.thrift.protocol.TProtocol
import scala.reflect.ClassTag
import scala.reflect.ManifestFactory
import scala.reflect.classTag

class CodecRetrievalSpec extends JMockSpec with EvalHelper {
  "CodecRetrievalSpec" should {
    "retrieve codec for ordinary struct given a class" in { _ =>
      ThriftStructCodec.forStructClass(classOf[includes.a.thriftscala.Address]) must be(
        includes.a.thriftscala.Address)
    }

    "retrieve codec for ordinary struct given a manifest" in { _ =>
      ThriftStructCodec.forStructClassTag(manifest[includes.a.thriftscala.Address]) must be(
        includes.a.thriftscala.Address)
    }

    "retrieve codec for ordinary struct given a ClassTag" in { _ =>
      ThriftStructCodec.forStructClassTag(classTag[includes.a.thriftscala.Address]) must be(
        includes.a.thriftscala.Address)
    }

    "retrieve codec for immutable struct given a class" in { _ =>
      ThriftStructCodec.forStructClass(classOf[includes.a.thriftscala.Address.Immutable]) must be(
        includes.a.thriftscala.Address.Immutable)
    }

    "retrieve codec for immutable struct given a manifest" in { _ =>
      ThriftStructCodec.forStructClassTag(
        manifest[includes.a.thriftscala.Address.Immutable]) must be(
        includes.a.thriftscala.Address.Immutable)
    }

    "retrieve codec for immutable struct given a ClassTag" in { _ =>
      ThriftStructCodec.forStructClassTag(
        classTag[includes.a.thriftscala.Address.Immutable]) must be(
        includes.a.thriftscala.Address.Immutable)
    }

    "retrieve codec for union trait given a class" in { _ =>
      ThriftStructCodec.forStructClass(classOf[includes.a.thriftscala.City]) must be(
        includes.a.thriftscala.City)
    }

    "retrieve codec for union trait given a manifest" in { _ =>
      ThriftStructCodec.forStructClassTag(manifest[includes.a.thriftscala.City]) must be(
        includes.a.thriftscala.City)
    }

    "retrieve codec for union trait given a ClassTag" in { _ =>
      ThriftStructCodec.forStructClassTag(classTag[includes.a.thriftscala.City]) must be(
        includes.a.thriftscala.City)
    }

    "retrieve codec for union case class given a class" in { _ =>
      ThriftStructCodec.forStructClass(classOf[includes.a.thriftscala.City.CityState]) must be(
        includes.a.thriftscala.City)
    }

    "retrieve codec for union case class given a manifest" in { _ =>
      ThriftStructCodec.forStructClassTag(manifest[includes.a.thriftscala.City.CityState]) must be(
        includes.a.thriftscala.City)
    }

    "retrieve codec for union case class given a ClassTag" in { _ =>
      ThriftStructCodec.forStructClassTag(classTag[includes.a.thriftscala.City.CityState]) must be(
        includes.a.thriftscala.City)
    }

    "fail to retrieve codec for a class with no companion given a class" in { _ =>
      val c = (new ThriftStruct {
        override def write(oprot: TProtocol): Unit = ???
      }).getClass
      assertThrows[ClassNotFoundException] {
        ThriftStructCodec.forStructClass(c)
      }
    }

    "fail to retrieve codec for a class with no companion given a manifest" in { _ =>
      val m = ManifestFactory.classType[ThriftStruct]((new ThriftStruct {
        override def write(oprot: TProtocol): Unit = ???
      }).getClass)
      assertThrows[ClassNotFoundException] {
        ThriftStructCodec.forStructClassTag(m)
      }
    }

    "fail to retrieve codec for a class with no companion given a ClassTag" in { _ =>
      val ct = ClassTag[ThriftStruct]((new ThriftStruct {
        override def write(oprot: TProtocol): Unit = ???
      }).getClass)
      assertThrows[ClassNotFoundException] {
        ThriftStructCodec.forStructClassTag(ct)
      }
    }
  }
}
