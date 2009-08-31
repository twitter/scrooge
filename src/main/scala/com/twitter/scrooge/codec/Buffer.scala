package com.twitter.scrooge.codec

import java.nio.ByteOrder
import org.apache.mina.core.buffer.IoBuffer

class Buffer {
  val buffer = IoBuffer.allocate(1024, false)
  buffer.setAutoExpand(true)
  buffer.order(ByteOrder.BIG_ENDIAN)

  def writeBoolean(n: Boolean) = buffer.put(if (n) 1.toByte else 0.toByte)

  def writeByte(n: Byte) = buffer.put(n)

  def writeI16(n: Short) = buffer.putShort(n)

  def writeI32(n: Int) = buffer.putInt(n)

  def writeI64(n: Long) = buffer.putLong(n)

  def writeDouble(n: Double) = buffer.putDouble(n)

  def writeString(s: String) = {
    writeI32(s.length)
    buffer.put(s.getBytes)
  }

  def writeBinary(x: Array[Byte]) = {
    writeI32(x.size)
    buffer.put(x)
  }

  def writeListHeader(itemtype: Int, size: Int) = {
    buffer.put(itemtype.toByte)
    buffer.putInt(size)
  }

  def writeMapHeader(keytype: Int, valuetype: Int, size: Int) = {
    buffer.put(keytype.toByte)
    buffer.put(valuetype.toByte)
    buffer.putInt(size)
  }

  def writeSetHeader(itemtype: Int, size: Int) = {
    buffer.put(itemtype.toByte)
    buffer.putInt(size)
  }

  def writeFieldHeader(header: FieldHeader) = {
    buffer.put(header.ftype.toByte)
    if (header.ftype != Type.STOP) {
      buffer.putShort(header.fid.toShort)
    }
  }

  def writeRequestHeader(header: RequestHeader) = {
    buffer.putShort(Codec.VERSION_1.toShort)
    buffer.putShort(header.messageType.toShort)
    writeString(header.methodName)
    buffer.putInt(header.sequenceId)
  }
}
