package com.twitter.scrooge

import org.apache.thrift.protocol.TType
import org.specs.SpecificationWithJUnit
import thrift.test._

// This is a cross project test and I feel bad for putting it here
// but scrooge-generator already has all the infrastructure to do
// generation and testing and anyway requires scrooge-runtime for
// tests.
class ThriftStructMetaDataSpec extends SpecificationWithJUnit {
  "Provide useful metadata" in {
    val s = Xtruct3("some string", 2, 3, 4L)
    val metaData = Xtruct3.metaData

    assert(metaData.codecClass == Xtruct3.getClass) // mustEqual doesn't work here
    metaData.structClassName mustEqual "thrift.test.Xtruct3"
    metaData.structName mustEqual "Xtruct3"
    metaData.structClass mustEqual classOf[Xtruct3]

    val Seq(f1, f2, _, _)  = metaData.fields.sortBy(_.id)

    f1.name mustEqual "string_thing"
    f1.id mustEqual 1
    f1.`type` mustEqual TType.STRING
    f1.getValue[String](s) mustEqual "some string"

    f2.name mustEqual "changed"
    f2.id mustEqual 4
    f2.`type` mustEqual TType.I32
    f2.getValue[Int](s) mustEqual 2
  }
}
