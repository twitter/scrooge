package com.twitter.scrooge

import com.twitter.scrooge.testutil.Spec

class TReusableMemoryTransportSpec extends Spec {

  "is reusable" in {
    val cap = 10
    val trans = TReusableMemoryTransport(cap)
    trans.currentCapacity must be(cap)
    var bytesRead = trans.read(new Array[Byte](1), 0, 999)
    bytesRead must be(0)

    val stringInBytes = "abcde".getBytes("UTF-8")
    trans.write(stringInBytes)
    trans.numWrittenBytes must be(5)

    val read = new Array[Byte](100)
    bytesRead = trans.read(read, 0, 999)
    bytesRead must be(5)
    read.take(bytesRead).mkString must be(stringInBytes.mkString)

    trans.reset()

    trans.numWrittenBytes must be(0)
    bytesRead = trans.read(new Array[Byte](1), 0, 999)
    bytesRead must be(0)

    trans.write(stringInBytes)
    trans.numWrittenBytes must be(5)
  }

}
