package com.twitter.scrooge

import com.twitter.scrooge.serializer.thriftscala.SerializerStringTest
import com.twitter.scrooge.serializer.thriftscala.SerializerListTest
import com.twitter.scrooge.serializer.thriftscala.SerializerSetTest
import com.twitter.scrooge.serializer.thriftscala.SerializerMapTest
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.apache.thrift.TException

@RunWith(classOf[JUnitRunner])
class ThriftBadDataTest extends AnyFunSuite {

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

  // This is how 'Structure' is encoded.
  //Binary protocol field header and field value:
  //+--------+--------+--------+--------+...+--------+
  //|tttttttt| field id        | field value         |
  //+--------+--------+--------+--------+...+--------+
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
    assert(
      readStrLength == inputString.size,
      "Ascii string, if in right place string lengths should match"
    )

    // Encode a very large number in place so it should OOM trying to allocate
    encodeI32(bytes, 3, Int.MaxValue)
    assert(decodeI32(bytes, 3) == Int.MaxValue, "Should get back the big number we encoded")
    bytes
  }

  //Binary protocol list (5+ bytes) and elements:
  //+--------+--------+--------+--------+--------+--------+...+--------+
  //|tttttttt| size                              | elements            |
  //+--------+--------+--------+--------+--------+--------+...+--------+
  private[this] def setupBadList(inputList: List[Int]): Array[Byte] = {
    val instance = SerializerListTest(inputList)
    val tss = BinaryThriftStructSerializer(SerializerListTest)

    val bytes = tss.toBytes(instance)

    // We assert a series of extra things here to ensure we are manipulating this structure
    // as expected. See above encoded Structure. Type within structure starts at 3rd byte.
    val tpe = bytes(0)
    val fieldId = decodeI16(bytes, 1)
    val listType = bytes(3)
    val readListLength = decodeI32(bytes, 4)
    assert(tpe == 15, "Should match Struct-type of List")
    assert(fieldId == 1, "Should field ID 1")
    assert(listType == 8, "Should match List-type of Int")
    assert(
      readListLength == inputList.size,
      "List , if in right place List lengths should match"
    )
    // Encode a very large number in place so it should OOM trying to allocate
    encodeI32(bytes, 4, Int.MaxValue)
    assert(decodeI32(bytes, 4) == Int.MaxValue, "Should get back the big number we encoded")
    bytes
  }

  //Binary protocol set (5+ bytes) and elements:
  //+--------+--------+--------+--------+--------+--------+...+--------+
  //|tttttttt| size                              | elements            |
  //+--------+--------+--------+--------+--------+--------+...+--------+
  private[this] def setupBadSet(inputSet: Set[Int]): Array[Byte] = {
    val instance = SerializerSetTest(inputSet)
    val tss = BinaryThriftStructSerializer(SerializerSetTest)

    val bytes = tss.toBytes(instance)

    // We assert a series of extra things here to ensure we are manipulating this structure
    // as expected. See above encoded Structure. Type within structure starts at 3rd byte.
    val tpe = bytes(0)
    val fieldId = decodeI16(bytes, 1)
    val setType = bytes(3)
    val readSetLength = decodeI32(bytes, 4)
    assert(tpe == 14, "Should match Struct-type of Set")
    assert(fieldId == 1, "Should field ID 1")
    assert(setType == 8, "Should match Set-type of Int")
    assert(
      readSetLength == inputSet.size,
      "Set, if in right place Set lengths should match"
    )
    // Encode a very large number in place so it should OOM trying to allocate
    encodeI32(bytes, 4, Int.MaxValue)
    assert(decodeI32(bytes, 4) == Int.MaxValue, "Should get back the big number we encoded")
    bytes
  }

  //Binary protocol map (6+ bytes) and key value pairs:
  //+--------+--------+--------+--------+--------+--------+--------+...+--------+
  //|kkkkkkkk|vvvvvvvv| size                              | key value pairs     |
  //+--------+--------+--------+--------+--------+--------+--------+...+--------+
  private[this] def setupBadMap(inputMap: Map[Int, Int]): Array[Byte] = {
    val instance = SerializerMapTest(inputMap)
    val tss = BinaryThriftStructSerializer(SerializerMapTest)

    val bytes = tss.toBytes(instance)

    // We assert a series of extra things here to ensure we are manipulating this structure
    // as expected. See above encoded Structure. Type within structure starts at 3rd byte.
    val tpe = bytes(0)
    val fieldId = decodeI16(bytes, 1)
    val keyType = bytes(3)
    val valType = bytes(4)
    val readMapLength = decodeI32(bytes, 5)
    assert(tpe == 13, "Should match Struct-type of Map")
    assert(fieldId == 1, "Should field ID 1")
    assert(keyType == 8, "Should match Map-type of Key of type Int")
    assert(valType == 8, "Should match Map-type of Value of type Int")
    assert(
      readMapLength == inputMap.size,
      "Map size, if in right place Map lengths should match"
    )
    // Encode a very large number in place so it should OOM trying to allocate
    encodeI32(bytes, 5, Int.MaxValue)
    assert(decodeI32(bytes, 5) == Int.MaxValue, "Should get back the big number we encoded")
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

  test("LazyBinaryThriftStructSerializer does not throw when we lazy later decode a string") {
    val inputString = "asdfbd 123rfsd"
    val instance = SerializerStringTest(inputString)
    val tss = BinaryThriftStructSerializer(SerializerStringTest)

    val goodBytes = tss.toBytes(instance)

    val lazyTss = LazyBinaryThriftStructSerializer(SerializerStringTest)
    val decoded = lazyTss.fromBytes(goodBytes)
    assert(decoded.strField == inputString)
  }

  test("LazyBinaryThriftStructSerializer does not OOM in bad String decode case") {
    // This ends up being pretty contrived, but someone will manage to hit it.
    val inputString = "asdfbd 123rfsd"
    val badBytes = setupBadByteArray(inputString)

    val transport = new TArrayByteTransport
    val proto = new TLazyBinaryProtocol(transport)
    intercept[TException] {
      proto.decodeString(badBytes, 3)
    }
  }

  test("LazyBinaryThriftStructSerializer does not OOM in bad List decode case") {
    // This ends up being pretty contrived, but someone will manage to hit it.
    val inputList = List(42)
    val badBytes = setupBadList(inputList)

    val transport = TArrayByteTransport(badBytes)
    val proto = new TLazyBinaryProtocol(transport)
    proto.readStructBegin
    proto.readFieldBegin
    intercept[TException] {
      proto.readListBegin
    }
  }

  test("LazyBinaryThriftStructSerializer does not OOM in bad Set decode case") {
    // This ends up being pretty contrived, but someone will manage to hit it.
    val inputSet = Set(42)
    val badBytes = setupBadSet(inputSet)

    val transport = TArrayByteTransport(badBytes)
    val proto = new TLazyBinaryProtocol(transport)
    proto.readStructBegin
    proto.readFieldBegin
    intercept[TException] {
      proto.readSetBegin
    }
  }

  test("LazyBinaryThriftStructSerializer does not OOM in bad Map decode case") {
    // This ends up being pretty contrived, but someone will manage to hit it.
    val inputMap = Map(42 -> 42)
    val badBytes = setupBadMap(inputMap)

    val transport = TArrayByteTransport(badBytes)
    val proto = new TLazyBinaryProtocol(transport)
    proto.readStructBegin
    proto.readFieldBegin
    intercept[TException] {
      proto.readMapBegin
    }
  }
}
