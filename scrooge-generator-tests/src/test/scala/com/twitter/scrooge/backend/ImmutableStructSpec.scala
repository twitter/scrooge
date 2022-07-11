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
      val nested = NestedXtruct(
        xtruct,
        xtruct2,
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
      copied.x2.structThing must not be (nested.x2.structThing) //swapped
      copied.x2 must not be (nested.x2)
      copied.x3 must be(nested.x3)
    }

    "have populated metaData" in {
      val expectedFieldNames = Set(
        "string_thing",
        "byte_thing",
        "i32_thing",
        "i64_thing"
      )
      Xtruct.Immutable.metaData.fields.map(_.name).toSet must be(expectedFieldNames)
    }

    "unset non-optional field" in {
      val xtruct = Xtruct(
        "string_thing",
        10.toByte,
        100,
        1000L
      )
      xtruct.unsetField(4) must be(Xtruct("string_thing", 0.toByte, 100, 1000L))
    }

    "unset optional field" in {
      val boolTest = OptionalInt(
        "my_name",
        Some(32)
      )
      boolTest.unsetField(2) must be(OptionalInt("my_name", None))
    }

    "unset fields" in {
      val xtruct = Xtruct(
        "string_thing",
        10.toByte,
        100,
        1000L
      )

      xtruct.unsetFields(Set(1, 4, 9)) must be(Xtruct(null, 0.toByte, 0, 1000L))
    }
  }
}
