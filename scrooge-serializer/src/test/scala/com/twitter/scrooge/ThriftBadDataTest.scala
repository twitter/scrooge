package com.twitter.scrooge

import com.twitter.scrooge.serializer.thriftscala.SerializerStringTest
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.apache.thrift.TException

@RunWith(classOf[JUnitRunner])
class ThriftBadDataTest extends FunSuite {

  private[this] def encodeI32(buf: Array[Byte], offset: Int, i32: Int): Unit = {
    buf(offset + 0) = (0xff & (i32 >> 24)).toByte
    buf(offset + 1) = (0xff & (i32 >> 16)).toByte
    buf(offset + 2) = (0xff & (i32 >> 8)).toByte
    buf(offset + 3) = (0xff & (i32)).toByte
  }

  private[this] def decodeI32(buf: Array[Byte], off: Int): Int =
    ((buf(off) & 0xff) << 24) |
      ((buf(off + 1) & 0xff) << 16) |
      ((buf(off + 2) & 0xff) << 8) |
      ((buf(off + 3) & 0xff))

  private[this] def decodeI16(buf: Array[Byte], offset: Int): Short =
    (((buf(offset) & 0xff) << 8) | ((buf(offset + 1) & 0xff))).toShort


  private[this] def setupBadByteArray(inputString: String): Array[Byte] = {
    val instance = SerializerStringTest(inputString)
    val tss = BinaryThriftStructSerializer(SerializerStringTest)

    val bytes = tss.toBytes(instance)

    // In TBinary Protocol the struct itself has no header
    // The type is the first byte
    // followed by 2 bytes with the field ID

    // We assert a series of extra things here to ensure we are manipulating this structure
    // as expected
    val tpe = bytes(0)
    val fieldId = decodeI16(bytes, 1)
    val readStrLength = decodeI32(bytes, 3)
    assert(tpe == 11, "Should match type of string")
    assert(fieldId == 1, "Should field ID 1")
    assert(readStrLength == inputString.size, "Ascii string, if in right place string lengths should match")

    // Encode a very large number in place so it should OOM trying to allocate
    encodeI32(bytes, 3, Int.MaxValue)
    assert(decodeI32(bytes, 3) == Int.MaxValue, "Should get back the big number we encoded")
    bytes
  }

  test("BinaryThriftStructSerializer throws a TException with string payloads too long") {
    val baseInputString = "asdfbd 123rfsd"
    val tss = BinaryThriftStructSerializer(SerializerStringTest)
    val badBytes = setupBadByteArray(baseInputString)
    intercept[TException] {
      tss.fromBytes(badBytes)
    }
  }

  test("LazyBinaryThriftStructSerializer throws a TException with string payloads too long") {
    val baseInputString = "asdfbd 123rfsd"
    val tss = LazyBinaryThriftStructSerializer(SerializerStringTest)
    val badBytes = setupBadByteArray(baseInputString)
    intercept[TException] {
      tss.fromBytes(badBytes)
    }
  }

}
