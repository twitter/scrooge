package com.twitter.scrooge.integration

import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.TMemoryBuffer
import com.twitter.scrooge.{integration_apache => apacheGen}
import com.twitter.scrooge.{integration_scala => scroogeGen}
import com.twitter.scrooge.testutil.Spec

// TODO CSL-401: test apache service/Scrooge client and Scrooge service/Apache client
class ScalaIntegrationSpec extends Spec {
  "Apache" should {
    "transfer struct to Scrooge" in {
      val protocol = new TBinaryProtocol(new TMemoryBuffer(10000))
      val apacheStruct = new apacheGen.Bonk_struct("howdy world", 123)
      apacheStruct.write(protocol)
      val scroogeStruct = scroogeGen.BonkStruct.decode(protocol)
      // test transferred values
      scroogeStruct.message must be("howdy world")
      scroogeStruct.intThing must be(123)
      // test transferred names
      scroogeGen.BonkStruct.MessageField.name must be(
        apacheStruct.fieldForId(1).getFieldName) // == "message"
      scroogeGen.BonkStruct.IntThingField.name must be(
        apacheStruct.fieldForId(2).getFieldName) // == "int_thing"
    }

    "transfer union to Scrooge" in {
      val protocol = new TBinaryProtocol(new TMemoryBuffer(10000))
      val apacheStruct = new apacheGen.Bonk_struct("howdy world", 123)
      val apacheUnion = apacheGen.bonk_or_bool_union.bonk(apacheStruct)
      apacheUnion.write(protocol)
      val scroogeUnion = scroogeGen.BonkOrBoolUnion.decode(protocol)

      // test transferred values
      val scroogeStruct = scroogeUnion.asInstanceOf[scroogeGen.BonkOrBoolUnion.Bonk]
      scroogeStruct.bonk must not be(null)
      scroogeStruct.bonk.message must be("howdy world")
      scroogeStruct.bonk.intThing must be(123)
      // test transferred names
      scroogeGen.BonkOrBoolUnion.Union.name must be("bonk_or_bool_union")
      scroogeGen.BonkOrBoolUnion.BonkField.name must be(
        apacheUnion.fieldForId(1).getFieldName) // == "bonk"
      scroogeGen.BonkOrBoolUnion.BoolThingField.name must be(
        apacheUnion.fieldForId(2).getFieldName) // == "bool_thing"

    }
  }

  "Scrooge" should {
    "transfer struct to Apache" in {
      val protocol = new TBinaryProtocol(new TMemoryBuffer(10000))
      val scroogeStruct = scroogeGen.BonkStruct("howdy world", 123)
      scroogeGen.BonkStruct.encode(scroogeStruct, protocol)
      val apacheStruct = new apacheGen.Bonk_struct()
      apacheStruct.read(protocol)
      // test transferred values
      apacheStruct.getInt_thing must be(123)
      apacheStruct.getMessage must be("howdy world")
      // test transferred names
      apacheStruct.fieldForId(1).getFieldName must be(
        scroogeGen.BonkStruct.MessageField.name) // == "message"
      apacheStruct.fieldForId(2).getFieldName must be(
        scroogeGen.BonkStruct.IntThingField.name) // == "int_thing"
    }

    "transfer union to Apache" in {
      val protocol = new TBinaryProtocol(new TMemoryBuffer(10000))
      val scroogeStruct = scroogeGen.BonkStruct("howdy world", 123)
      val scroogeUnion = scroogeGen.BonkOrBoolUnion.Bonk(scroogeStruct)
      scroogeGen.BonkOrBoolUnion.encode(scroogeUnion, protocol)
      val apacheUnion = new apacheGen.bonk_or_bool_union()
      apacheUnion.read(protocol)
      // test transferred values
      val bonk = apacheUnion.getBonk
      bonk.getMessage must be("howdy world")
      bonk.getInt_thing must be(123)
      intercept[RuntimeException] {
        apacheUnion.getBool_thing
      }
      // test transferred names
      apacheUnion.fieldForId(1).getFieldName must be(
        scroogeGen.BonkOrBoolUnion.BonkField.name) // == "bonk"
      apacheUnion.fieldForId(2).getFieldName must be(
        scroogeGen.BonkOrBoolUnion.BoolThingField.name) // == "bonk"
    }
  }
}

