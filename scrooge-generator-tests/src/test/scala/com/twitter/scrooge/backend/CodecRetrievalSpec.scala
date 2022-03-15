package com.twitter.scrooge.backend

import com.twitter.scrooge._
import com.twitter.scrooge.testutil.EvalHelper
import com.twitter.scrooge.testutil.JMockSpec
import org.apache.thrift.protocol.TProtocol
import scala.reflect.ClassTag
import scala.reflect.ManifestFactory
import scala.reflect.classTag
import includes.a.thriftscala._

class CodecRetrievalSpec extends JMockSpec with EvalHelper {
  "CodecRetrievalSpec" should {
    "retrieve codec for ordinary struct given a class" in { _ =>
      ThriftStructCodec.forStructClass(classOf[Address]) must be(Address)
    }

    "retrieve codec for ordinary struct given a manifest" in { _ =>
      ThriftStructCodec.forStructClassTag(manifest[Address]) must be(Address)
    }

    "retrieve codec for ordinary struct given a ClassTag" in { _ =>
      ThriftStructCodec.forStructClassTag(classTag[Address]) must be(Address)
    }

    "retrieve codec for immutable struct given a class" in { _ =>
      ThriftStructCodec.forStructClass(classOf[Address.Immutable]) must be(Address.Immutable)
    }

    "retrieve codec for immutable struct given a manifest" in { _ =>
      ThriftStructCodec.forStructClassTag(manifest[Address.Immutable]) must be(Address.Immutable)
    }

    "retrieve codec for immutable struct given a ClassTag" in { _ =>
      ThriftStructCodec.forStructClassTag(classTag[Address.Immutable]) must be(Address.Immutable)
    }

    "retrieve codec for a subclass given a class" in { _ =>
      var a = new Address.Immutable("", new City.Zipcode(ZipCode(""))) {
        override def toString() = ""
      }
      a.getClass must not be (classOf[Address.Immutable])
      ThriftStructCodec.forStructClass(a.getClass) must be(Address.Immutable)
    }

    "retrieve codec for a subclass given a manifest" in { _ =>
      var a = new Address.Immutable("", new City.Zipcode(ZipCode(""))) {
        override def toString() = ""
      }
      a.getClass must not be (classOf[Address.Immutable])
      ThriftStructCodec.forStructClassTag(Manifest.classType(a.getClass)) must be(Address.Immutable)
    }

    "retrieve codec for a subclass given a ClassTag" in { _ =>
      var a = new Address.Immutable("", new City.Zipcode(ZipCode(""))) {
        override def toString() = ""
      }
      a.getClass must not be (classOf[Address.Immutable])
      ThriftStructCodec.forStructClassTag(ClassTag(a.getClass)) must be(Address.Immutable)
    }

    "retrieve codec for union trait given a class" in { _ =>
      ThriftStructCodec.forStructClass(classOf[City]) must be(City)
    }

    "retrieve codec for union trait given a manifest" in { _ =>
      ThriftStructCodec.forStructClassTag(manifest[City]) must be(City)
    }

    "retrieve codec for union trait given a ClassTag" in { _ =>
      ThriftStructCodec.forStructClassTag(classTag[City]) must be(City)
    }

    "retrieve codec for union case class given a class" in { _ =>
      ThriftStructCodec.forStructClass(classOf[City.CityState]) must be(City)
    }

    "retrieve codec for union case class given a manifest" in { _ =>
      ThriftStructCodec.forStructClassTag(manifest[City.CityState]) must be(City)
    }

    "retrieve codec for union case class given a ClassTag" in { _ =>
      ThriftStructCodec.forStructClassTag(classTag[City.CityState]) must be(City)
    }

    "fail to retrieve codec for a class with no companion given a class" in { _ =>
      val c = (new ThriftStruct {
        override def write(oprot: TProtocol): Unit = ???
      }).getClass
      assertThrows[IllegalArgumentException] {
        ThriftStructCodec.forStructClass(c)
      }
    }

    "fail to retrieve codec for a class with no companion given a manifest" in { _ =>
      val m = ManifestFactory.classType[ThriftStruct]((new ThriftStruct {
        override def write(oprot: TProtocol): Unit = ???
      }).getClass)
      assertThrows[IllegalArgumentException] {
        ThriftStructCodec.forStructClassTag(m)
      }
    }

    "fail to retrieve codec for a class with no companion given a ClassTag" in { _ =>
      val ct = ClassTag[ThriftStruct]((new ThriftStruct {
        override def write(oprot: TProtocol): Unit = ???
      }).getClass)
      assertThrows[IllegalArgumentException] {
        ThriftStructCodec.forStructClassTag(ct)
      }
    }
  }
}
