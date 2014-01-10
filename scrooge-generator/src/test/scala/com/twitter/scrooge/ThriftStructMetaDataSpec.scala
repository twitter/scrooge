package com.twitter.scrooge

import com.twitter.scrooge.testutil.Spec
import org.apache.thrift.protocol.TType
import scala.collection.{Map, Set}
import thrift.test._

// This is a cross project test and I feel bad for putting it here
// but scrooge-generator already has all the infrastructure to do
// generation and testing and anyway requires scrooge-runtime for
// tests.
class ThriftStructMetaDataSpec extends Spec {
  "Provide useful metadata" in {
    val s = XtructColl(Map(1 -> 2L), Seq("test"), Set(10.toByte), 123)
    val metaData = XtructColl.metaData

    assert(metaData.codecClass == XtructColl.getClass) // mustEqual doesn't work here
    metaData.structClassName must be("thrift.test.XtructColl")
    metaData.structName must be("XtructColl")
    metaData.structClass must be(classOf[XtructColl])

    val Seq(f1, f2, f3, f4) = metaData.fields.sortBy(_.id)

    f1.name must be("a_map")
    f1.id must be(1)
    f1.`type` must be(TType.MAP)
    f1.manifest must be(Some(implicitly[Manifest[Map[Int, Long]]]))
    f1.getValue[Map[Int, Long]](s) must be(Map(1 -> 2L))

    f2.name must be("a_list")
    f2.id must be(2)
    f2.`type` must be(TType.LIST)
    f2.manifest must be(Some(implicitly[Manifest[Seq[String]]]))
    f2.getValue[Seq[String]](s) must be(Seq("test"))

    f3.name must be("a_set")
    f3.id must be(3)
    f3.`type` must be(TType.SET)
    f3.manifest must be(Some(implicitly[Manifest[Set[Byte]]]))
    f3.getValue[Set[Byte]](s) must be(Set(10.toByte))

    f4.name must be("non_col")
    f4.id must be(4)
    f4.`type` must be(TType.I32)
    f4.manifest must be(Some(implicitly[Manifest[Int]]))
    f4.getValue[Int](s) must be(123)
  }
}
