package com.twitter.scrooge.backend

import com.twitter.scrooge.testutil.Spec
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.TMemoryBuffer

class ImmutableStructSpec extends Spec {

  "Scala objects" should {
    import thrift.test._
    "encode and decode" in {
      val protocol = new TBinaryProtocol(new TMemoryBuffer(10000))
      val originalBonk = Bonk("This is Bonk message", 13)
      Bonk.encode(originalBonk, protocol)
      Bonk.decode(protocol) must be(originalBonk)
    }

    "deep copy" in {
      // NestedXtruct contains Xtruct2 which contains Xtruct. We swap
      // the Xtruct object when doing deep copy
      val xtruct = Xtruct(
        "string_thing",
        10.toByte,
        100,
        1000L
      )
      val xtruct2 = Xtruct2(
        123.toByte,
        xtruct,
        321
      )
      val nested = NestedXtruct(xtruct, xtruct2,
        Xtruct3(
          "string_thing",
          456,
          654,
          999L
        )
      )
      val xtructSwapped = Xtruct(
        "not original xtruct string",
        100.toByte,
        1000,
        10000L
      )

      val copied = nested.copy(
        x2 = xtruct2.copy(
          structThing = xtructSwapped
        )
      )
      copied.x1 must be(nested.x1)
      copied.x2.structThing must not be(nested.x2.structThing) //swapped
      copied.x2 must not be(nested.x2)
      copied.x3 must be(nested.x3)
    }
  }

  "Java objects" should {
    import thrift.java_test._
    "encode and decode" in {
      val protocol = new TBinaryProtocol(new TMemoryBuffer(10000))
      val originalBonk = new Bonk("This is Bonk message", 13)
      Bonk.encode(originalBonk, protocol)
      Bonk.decode(protocol) must be(originalBonk)
    }

    "deep copy" in {
      val xtruct = new Xtruct(
        "string_thing",
        10.toByte,
        100,
        1000L
      )
      val xtruct2 = new Xtruct2(
        123.toByte,
        xtruct,
        321
      )
      val nested = new NestedXtruct(xtruct, xtruct2,
        new Xtruct3(
          "string_thing",
          456,
          654,
          999L
        )
      )
      val xtructSwapped = new Xtruct(
        "not original xtruct string",
        100.toByte,
        1000,
        10000L
      )

      val copied = nested.copy()
        .x2(xtruct2.copy().structThing(xtructSwapped).build())
        .build()

      copied.getX1 must be(nested.getX1)
      copied.getX2.getStructThing must not be(nested.getX2.getStructThing) //swapped
      copied.getX2 must not be(nested.getX2)
      copied.getX3 must be(nested.getX3)

      val copiedPartial = nested.copy().unsetX2().build()
      copiedPartial.isSetX2 must be(false)
    }
  }
}
