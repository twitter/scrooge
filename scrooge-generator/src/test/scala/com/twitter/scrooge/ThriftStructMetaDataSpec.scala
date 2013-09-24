package com.twitter.scrooge

import org.apache.thrift.protocol.TType
import org.specs.SpecificationWithJUnit
import scala.collection.{Map, Set}
import thrift.test._

// This is a cross project test and I feel bad for putting it here
// but scrooge-generator already has all the infrastructure to do
// generation and testing and anyway requires scrooge-runtime for
// tests.
class ThriftStructMetaDataSpec extends SpecificationWithJUnit {
  "Provide useful metadata" in {
    val s = XtructColl(Map(1 -> 2L), Seq("test"), Set(10.toByte), 123)
    val metaData = XtructColl.metaData

    assert(metaData.codecClass == XtructColl.getClass) // mustEqual doesn't work here
    metaData.structClassName mustEqual "thrift.test.XtructColl"
    metaData.structName mustEqual "XtructColl"
    metaData.structClass mustEqual classOf[XtructColl]

    val Seq(f1, f2, f3, f4) = metaData.fields.sortBy(_.id)

    f1.name mustEqual "a_map"
    f1.id mustEqual 1
    f1.`type` mustEqual TType.MAP
    f1.manifest mustEqual Some(implicitly[Manifest[Map[Int, Long]]])
    f1.getValue[Map[Int, Long]](s) mustEqual Map(1 -> 2L)

    f2.name mustEqual "a_list"
    f2.id mustEqual 2
    f2.`type` mustEqual TType.LIST
    f2.manifest mustEqual Some(implicitly[Manifest[Seq[String]]])
    f2.getValue[Seq[String]](s) mustEqual Seq("test")

    f3.name mustEqual "a_set"
    f3.id mustEqual 3
    f3.`type` mustEqual TType.SET
    f3.manifest mustEqual Some(implicitly[Manifest[Set[Byte]]])
    f3.getValue[Set[Byte]](s) mustEqual Set(10.toByte)

    f4.name mustEqual "non_col"
    f4.id mustEqual 4
    f4.`type` mustEqual TType.I32
    f4.manifest mustEqual Some(implicitly[Manifest[Int]])
    f4.getValue[Int](s) mustEqual 123
  }
}
