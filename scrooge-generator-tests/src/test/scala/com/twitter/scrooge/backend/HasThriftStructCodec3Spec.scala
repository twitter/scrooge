package com.twitter.scrooge.backend

import com.twitter.scrooge.testutil.Spec
import com.twitter.scrooge.{HasThriftStructCodec3, ThriftStructCodec3}
import thrift.test._

class HasThriftStructCodec3Spec extends Spec {

  "All ThriftStructs" should {
    "have a codec method via HasThriftStructCodec3" in {
      val struct = RequiredString("yo")
      struct.isInstanceOf[HasThriftStructCodec3[RequiredString]] must be(true)
      struct._codec.isInstanceOf[ThriftStructCodec3[RequiredString]] must be(true)

      val e = Xception(10, "yo")
      e.isInstanceOf[HasThriftStructCodec3[Xception]] must be(true)
      e._codec.isInstanceOf[ThriftStructCodec3[Xception]] must be(true)

      val union = EnumUnion.Text("yo")
      union.isInstanceOf[HasThriftStructCodec3[EnumUnion]] must be(true)
      union._codec.isInstanceOf[ThriftStructCodec3[EnumUnion]] must be(true)
    }
  }

}
