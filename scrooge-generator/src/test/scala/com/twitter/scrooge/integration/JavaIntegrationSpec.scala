package com.twitter.scrooge.integration

import org.specs.SpecificationWithJUnit
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.TMemoryBuffer
import com.twitter.scrooge.{integration_apache => apacheGen}
import com.twitter.scrooge.{integration_java => scroogeGen}

// TODO: CSL-405(union fields are not accessible from Java). Add union tests
class JavaIntegrationSpec extends SpecificationWithJUnit {
  "Apache" should {
    "transfer struct to Scrooge" in {
      val protocol = new TBinaryProtocol(new TMemoryBuffer(10000))
      val apacheStruct = new apacheGen.Bonk_struct("howdy world", 123)
      apacheStruct.write(protocol)
      val scroogeStruct = scroogeGen.BonkStruct.decode(protocol)
      // test transferred values
      scroogeStruct.getMessage mustEqual("howdy world")
      scroogeStruct.getIntThing mustEqual(123)
    }
    "transfer union to Scrooge" in {
    }
  }

  "Scrooge" should {
    "transfer struct to Apache" in {
      val protocol = new TBinaryProtocol(new TMemoryBuffer(10000))
      val scroogeStruct = new scroogeGen.BonkStruct("howdy world", 123)
      scroogeGen.BonkStruct.encode(scroogeStruct, protocol)
      val apacheStruct = new apacheGen.Bonk_struct()
      apacheStruct.read(protocol)
      // test transferred values
      apacheStruct.getInt_thing mustEqual(123)
      apacheStruct.getMessage mustEqual("howdy world")
    }
    "transfer union to Apache" in {
    }
  }
}
