package com.twitter.scrooge.internal

import com.twitter.scrooge.{TArrayByteTransport, TFieldBlob, TLazyBinaryProtocol, ThriftUnion}
import com.twitter.util.mock.Mockito
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.protocol.TCompactProtocol
import org.apache.thrift.protocol.TField
import org.apache.thrift.protocol.TProtocolException
import org.apache.thrift.protocol.TType
import org.apache.thrift.transport.TMemoryBuffer

import scala.collection.immutable
import org.scalatest.funsuite.AnyFunSuite

class TProtocolsTest extends AnyFunSuite with Mockito {

  test("writeSet and readSet") {
    val set = Set(1, 2, 3)
    val protos = TProtocols()

    val writeBuffer = new TMemoryBuffer(128)
    protos.writeSet(new TBinaryProtocol(writeBuffer), set, TType.I32, TProtocols.writeI32Fn)

    val readBuffer = TArrayByteTransport(writeBuffer.getArray)
    val result = protos.readSet(new TBinaryProtocol(readBuffer), TProtocols.readI32Fn)
    assert(set == result)
  }

  test("writeList and readList") {
    val list = List(1, 3, 5)
    val protos = TProtocols()

    val writeBuffer = new TMemoryBuffer(128)
    protos.writeList(new TBinaryProtocol(writeBuffer), list, TType.I32, TProtocols.writeI32Fn)

    val readBuffer = TArrayByteTransport(writeBuffer.getArray)
    val result = protos.readList(new TBinaryProtocol(readBuffer), TProtocols.readI32Fn)
    assert(list == result)
  }

  test("writeMap and readMap") {
    val map = Map("a" -> 1, "b" -> 3, "c" -> 5)
    val protos = TProtocols()

    val writeBuffer = new TMemoryBuffer(128)
    protos.writeMap(
      new TBinaryProtocol(writeBuffer),
      map,
      TType.STRING,
      TProtocols.writeStringFn,
      TType.I32,
      TProtocols.writeI32Fn)

    val readBuffer = TArrayByteTransport(writeBuffer.getArray)
    val result =
      protos.readMap(new TBinaryProtocol(readBuffer), TProtocols.readStringFn, TProtocols.readI32Fn)
    assert(map == result)
  }

  test("TProtocols.validateFieldType") {
    TProtocols.validateFieldType(TType.I16, TType.I16, "test")
    intercept[TProtocolException] {
      TProtocols.validateFieldType(TType.BOOL, TType.I16, "test")
    }
  }

  test("TProtocols.validateEnumFieldType") {
    TProtocols.validateEnumFieldType(TType.I32, "test")
    TProtocols.validateEnumFieldType(TType.ENUM, "test")
    intercept[TProtocolException] {
      TProtocols.validateEnumFieldType(TType.I16, "test")
    }
  }

  test("TProtocols.throwMissingRequiredField") {
    intercept[TProtocolException] {
      TProtocols.throwMissingRequiredField("struct", "field")
    }
  }

  // note: TFieldBlobs on pass throughs use the TCompactProtocol.
  test("TProtocols.readPassthroughField when passthroughs is null") {
    val writeBuffer = new TMemoryBuffer(128)
    val writeProto = new TCompactProtocol(writeBuffer)
    val i32data = 253
    writeProto.writeI32(i32data)
    val id: Short = 7
    val tfield = new TField("name", TType.I32, id)

    val readBuffer = TArrayByteTransport(writeBuffer.getArray)
    val builder = TProtocols.readPassthroughField(new TCompactProtocol(readBuffer), tfield, null)
    val passthroughs = builder.result()
    assert(1 == passthroughs.size)
    val readTField = passthroughs(id)
    assert(id == readTField.id)
    assert(tfield == readTField.field)

    val readProt = readTField.read
    val readI32 = readProt.readI32()
    // there should be no data left in the transport
    assert(readProt.getTransport.read(new Array[Byte](1), 0, 1) == 0)
    assert(i32data == readI32)
  }

  // note: TFieldBlobs on pass throughs use the TCompactProtocol.
  test("TProtocols.readPassthroughField when passthroughs is not null") {
    val writeBuffer = new TMemoryBuffer(128)
    val writeProto = new TCompactProtocol(writeBuffer)
    val i32data = 253
    writeProto.writeI32(i32data)
    val id: Short = 7
    val tfield = new TField("name", TType.I32, id)

    val readBuffer = TArrayByteTransport(writeBuffer.getArray)
    val otherId: Short = (id + 1).toShort
    val initialPassthroughs = immutable.Map.newBuilder[Short, TFieldBlob]
    initialPassthroughs += otherId -> TFieldBlob(
      new TField("other", TType.I32, otherId),
      Array.empty[Byte])
    val builder =
      TProtocols.readPassthroughField(new TCompactProtocol(readBuffer), tfield, initialPassthroughs)
    val passthroughs = builder.result()
    assert(2 == passthroughs.size)
    val readTField = passthroughs(id)
    assert(id == readTField.id)
    assert(tfield == readTField.field)

    val readI32 = readTField.read.readI32()
    assert(i32data == readI32)
  }

  test("TProtocols.finishReadingUnion when result is null") {
    val proto = new TBinaryProtocol(new TMemoryBuffer(8))
    val result: ThriftUnion = null
    intercept[TProtocolException] {
      TProtocols.finishReadingUnion(proto, TType.STOP, result)
    }
  }

  test("TProtocols.finishReadingUnion did not read stop") {
    val writeBuffer = new TMemoryBuffer(128)
    val writeProto = new TBinaryProtocol(writeBuffer)
    writeProto.writeFieldStop()
    writeProto.writeStructEnd()

    val readProto = new TBinaryProtocol(TArrayByteTransport(writeBuffer.getArray))
    TProtocols.finishReadingUnion(readProto, TType.I32, mock[ThriftUnion])
    succeed
  }

  test("TProtocols.finishReadingUnion did not read stop and has more than one field") {
    val writeBuffer = new TMemoryBuffer(128)
    val writeProto = new TBinaryProtocol(writeBuffer)
    writeProto.writeFieldBegin(new TField("another field", TType.I32, 2))
    writeProto.writeI32(111)
    writeProto.writeFieldStop()
    writeProto.writeStructEnd()

    val readProto = new TBinaryProtocol(TArrayByteTransport(writeBuffer.getArray))
    intercept[TProtocolException] {
      TProtocols.finishReadingUnion(readProto, TType.I32, mock[ThriftUnion])
    }
  }

  test("TProtocols.finishReadingUnion happy path") {
    val writeBuffer = new TMemoryBuffer(128)
    val writeProto = new TBinaryProtocol(writeBuffer)
    writeProto.writeStructEnd()

    val readProto = new TBinaryProtocol(TArrayByteTransport(writeBuffer.getArray))
    TProtocols.finishReadingUnion(readProto, TType.STOP, mock[ThriftUnion])
    succeed
  }

  test("readFieldBegin handles new ENUM type identifier") {
    val writeBuffer = new TMemoryBuffer(128)
    val writeProto = new TBinaryProtocol(writeBuffer)

    val newEnum = -1

    writeProto.writeByte(newEnum.toByte) // New ENUM type identifier
    writeProto.writeI16(1) // Field ID
    writeProto.writeI32(2) // Enum value

    val readBuffer = TArrayByteTransport(writeBuffer.getArray)
    val readProto = new TLazyBinaryProtocol(readBuffer)

    val field = readProto.readFieldBegin()
    assert(field.`type` == TType.ENUM)
    assert(field.id == 1)

    val enumValue = readProto.readI32()
    assert(enumValue == 2)
  }

  test("readFieldBegin handles old ENUM type identifier") {
    val writeBuffer = new TMemoryBuffer(128)
    val writeProto = new TBinaryProtocol(writeBuffer)

    writeProto.writeByte(TType.ENUM)
    writeProto.writeI16(1)
    writeProto.writeI32(2)

    val readBuffer = TArrayByteTransport(writeBuffer.getArray)
    val readProto = new TLazyBinaryProtocol(readBuffer)

    val field = readProto.readFieldBegin()
    assert(field.`type` == TType.ENUM)
    assert(field.id == 1)

    val enumValue = readProto.readI32()
    assert(enumValue == 2)
  }

}
