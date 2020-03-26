package com.twitter.scrooge.integration

import com.twitter.scrooge.integration_android.Bonk_struct
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.TMemoryBuffer
import com.twitter.scrooge.testutil.Spec
import com.twitter.scrooge.{integration_android => androidGen}
import com.twitter.scrooge.{integration_scala => scroogeGen}

class AndroidIntegrationSpec extends Spec {
  "Android" should {
    "transfer struct to Scrooge" in {
      val protocol = new TBinaryProtocol(new TMemoryBuffer(10000))
      val builder = new androidGen.Bonk_struct.Builder
      val androidStruct = builder
        .set(Bonk_struct.INT_THING, 123)
        .set(Bonk_struct.MESSAGE, "howdy world")
        .build()
      androidStruct.write(protocol)

      val scroogeStruct = scroogeGen.BonkStruct.decode(protocol)
      // test transferred values
      scroogeStruct.message must be("howdy world")
      scroogeStruct.intThing must be(123)
      // test transferred names
      scroogeGen.BonkStruct.MessageField.name must be(
        androidStruct.fieldForId(1).getFieldName
      ) // == "message"
      scroogeGen.BonkStruct.IntThingField.name must be(
        androidStruct.fieldForId(2).getFieldName
      ) // == "int_thing"
    }
    "transfer union to Scrooge" in {
      val protocol = new TBinaryProtocol(new TMemoryBuffer(10000))
      val builder = new androidGen.Bonk_struct.Builder
      val androidStruct = builder
        .set(Bonk_struct.INT_THING, 123)
        .set(Bonk_struct.MESSAGE, "howdy world")
        .build()
      val androidUnion =
        new androidGen.bonk_or_bool_union(androidGen.bonk_or_bool_union.BONK, androidStruct)
      androidUnion.write(protocol)
      val scroogeUnion = scroogeGen.BonkOrBoolUnion.decode(protocol)

      // test transferred values
      val scroogeStruct = scroogeUnion.asInstanceOf[scroogeGen.BonkOrBoolUnion.Bonk]
      scroogeStruct.bonk must not be (null)
      scroogeStruct.bonk.message must be("howdy world")
      scroogeStruct.bonk.intThing must be(123)
      // test transferred names
      scroogeGen.BonkOrBoolUnion.Union.name must be("bonk_or_bool_union")
      scroogeGen.BonkOrBoolUnion.BonkField.name must be(
        androidUnion.fieldForId(1).getFieldName
      ) // == "bonk"
      scroogeGen.BonkOrBoolUnion.BoolThingField.name must be(
        androidUnion.fieldForId(2).getFieldName
      ) // == "bool_thing"
    }
  }

  "Scrooge" should {
    "transfer struct to Android" in {
      val protocol = new TBinaryProtocol(new TMemoryBuffer(10000))
      val scroogeStruct = scroogeGen.BonkStruct("howdy world", 123)
      scroogeGen.BonkStruct.encode(scroogeStruct, protocol)
      val androidStruct = new androidGen.Bonk_struct()
      androidStruct.read(protocol)
      val int_thing: Integer = androidStruct.get(androidGen.Bonk_struct.INT_THING)
      int_thing must be(123)
      val message: String = androidStruct.get(androidGen.Bonk_struct.MESSAGE)
      message must be("howdy world")
    }

    "transfer union to Android" in {
      val protocol = new TBinaryProtocol(new TMemoryBuffer(10000))
      val scroogeStruct = scroogeGen.BonkStruct("howdy world", 123)
      val scroogeUnion = scroogeGen.BonkOrBoolUnion.Bonk(scroogeStruct)
      scroogeGen.BonkOrBoolUnion.encode(scroogeUnion, protocol)
      val androidUnion = new androidGen.bonk_or_bool_union()
      androidUnion.read(protocol)

      val setField = androidUnion.getSetField()

      val value = androidUnion.getFieldValue(setField)
      androidUnion.getFieldValue(setField).isInstanceOf[androidGen.Bonk_struct] must be(true)

      val value2 = value.asInstanceOf[androidGen.Bonk_struct]
      val int_thing: Integer = value2.get(androidGen.Bonk_struct.INT_THING)
      int_thing must be(123)
      val message: String = value2.get(androidGen.Bonk_struct.MESSAGE)
      message must be("howdy world")
    }
  }
}
