package com.twitter.scrooge

import com.twitter.scrooge.serializer.thriftscala.SerializerTest
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ThriftStructSerializerTest extends FunSuite {

  test("toBytes and fromBytes round trip") {
    val instance = SerializerTest(5)

    val tss = BinaryThriftStructSerializer(SerializerTest)

    val bytes = tss.toBytes(instance)
    val andBack = tss.fromBytes(bytes)
    assert(instance == andBack)
  }

  test("transportTooBig counter") {
    val startCount = ThriftStructSerializer.transportTooBig.get()
    val instance = SerializerTest(5)
    maxReusableBufferSize.let(0) {
      val tss = BinaryThriftStructSerializer(SerializerTest)
      val bytes = tss.toBytes(instance)
      val andBack = tss.fromBytes(bytes)
      assert(instance == andBack)
    }
    assert(ThriftStructSerializer.transportTooBig.get() == startCount + 1)
  }

}
