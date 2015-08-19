package com.twitter.scrooge

import java.io.UnsupportedEncodingException
import java.lang.StringIndexOutOfBoundsException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import org.apache.thrift.protocol._
import org.apache.thrift.TException

/**
 * This is an implementation of the LazyTProtocol trait in scrooge-core
 * This is not thread safe and maintains state. It also heavily uses inline annotations and marks
 * things as final where possible to avoid virtual indirects.
 * Its in a benchmark package as a POC, we may want to do something a bit different
 * when it comes to a version for scrooge-serializer or elsewhere.
 * Though it is a fully functional protocol that will deserialize/serialize any thrift.
 */
object TLazyBinaryProtocol {
  private val AnonymousStruct: TStruct = new TStruct()
  private val utf8Charset = Charset.forName("UTF-8")
}

class TLazyBinaryProtocol(transport: TArrayByteTransport) extends TBinaryProtocol(transport) with LazyTProtocol {
  import TLazyBinaryProtocol._

  @inline
  final def writeRaw(buf: Array[Byte], offset: Int, len: Int): Unit = {
    transport.write(buf, offset, len)
  }

  override def writeFieldBegin(field: TField) = {
    val buf = transport.getBuffer(3)
    val offset = transport.writerOffset
    buf(offset) = field.`type`
    innerWriteI16(buf, offset + 1, field.id)
  }

  @inline
  override def writeFieldEnd(): Unit = ()

  @inline
  override def writeFieldStop(): Unit = {
    writeByte(TType.STOP)
  }

  override def writeMapBegin(map: TMap): Unit = {
    val buf = transport.getBuffer(6)
    val offset = transport.writerOffset
    buf(offset) = map.keyType
    buf(offset + 1) = map.valueType
    innerWriteI32(buf, offset + 2, map.size)
  }

  @inline
  override def writeMapEnd(): Unit = ()

  override def writeListBegin(list: TList): Unit = {
    val buf = transport.getBuffer(5)
    val offset = transport.writerOffset
    buf(offset) = list.elemType
    innerWriteI32(buf, offset + 1, list.size)
  }

  @inline
  override def writeListEnd(): Unit = ()

  override def writeSetBegin(set: TSet): Unit = {
    val buf = transport.getBuffer(5)
    val offset = transport.writerOffset
    buf(offset) = set.elemType
    innerWriteI32(buf, offset + 1, set.size)
  }

  @inline
  override def writeSetEnd(): Unit = ()

  @inline
  override def writeBool(b: Boolean): Unit = {
    writeByte(if (b) 1 else 0)
  }

  @inline
  override def writeByte(b: Byte): Unit = {
    val buf = transport.getBuffer(1)
    val offset = transport.writerOffset
    buf(offset) = b
  }

  @inline private[this] final def innerWriteI16(buf: Array[Byte], offset: Int, i16: Short): Unit = {
    buf(offset + 0) = (0xff & (i16 >> 8)).toByte
    buf(offset + 1) = (0xff & (i16)).toByte
  }

  override def writeI16(i16: Short): Unit = {
    val buf = transport.getBuffer(2)
    val offset = transport.writerOffset
    innerWriteI16(buf, offset, i16)
  }

  @inline private[this] final def innerWriteI32(buf: Array[Byte], offset: Int, i32: Int): Unit = {
    buf(offset + 0) = (0xff & (i32 >> 24)).toByte
    buf(offset + 1) = (0xff & (i32 >> 16)).toByte
    buf(offset + 2) = (0xff & (i32 >> 8)).toByte
    buf(offset + 3) = (0xff & (i32)).toByte
  }

  override def writeI32(i32: Int): Unit = {
    val buf = transport.getBuffer(4)
    val offset = transport.writerOffset
    innerWriteI32(buf, offset, i32)
  }

  override def writeI64(i64: Long): Unit = {
    val buf = transport.getBuffer(8)
    val offset = transport.writerOffset
    buf(offset + 0) = (0xff & (i64 >> 56)).toByte
    buf(offset + 1) = (0xff & (i64 >> 48)).toByte
    buf(offset + 2) = (0xff & (i64 >> 40)).toByte
    buf(offset + 3) = (0xff & (i64 >> 32)).toByte
    buf(offset + 4) = (0xff & (i64 >> 24)).toByte
    buf(offset + 5) = (0xff & (i64 >> 16)).toByte
    buf(offset + 6) = (0xff & (i64 >> 8)).toByte
    buf(offset + 7) = (0xff & (i64)).toByte
  }

  override def writeDouble(dub: Double): Unit = {
    writeI64(java.lang.Double.doubleToLongBits(dub))
  }

  override def writeString(str: String): Unit = {
    try {
      val data: Array[Byte] = str.getBytes(utf8Charset)
      val buf = transport.getBuffer(data.length + 4)
      val offset = transport.writerOffset
      innerWriteI32(buf, offset, data.length)
      System.arraycopy(data, 0, buf, offset + 4, data.length)
    } catch {
      case uex: UnsupportedEncodingException =>
        throw new TException("JVM DOES NOT SUPPORT UTF-8")
    }
  }

  override def writeBinary(bin: ByteBuffer): Unit = {
    val length: Int = bin.limit() - bin.position() - bin.arrayOffset()
    val buf = transport.getBuffer(length + 4)
    val offset = transport.writerOffset
    innerWriteI32(buf, offset, length)
    System.arraycopy(bin.array(), bin.position() + bin.arrayOffset(), buf, offset + 4, length)
  }

  /*
   * Reading methods
   */
  override def readMessageEnd: Unit = ()

  override def readStructBegin: TStruct = AnonymousStruct

  override def readStructEnd: Unit = ()

  override def readFieldBegin(): TField = {
    val tpe: Byte = readByte()
    val id: Short = if (tpe == TType.STOP) 0 else readI16()
    new TField("", tpe, id)
  }

  override def readFieldEnd(): Unit = ()

  override def readMapBegin(): TMap = new TMap(readByte(), readByte(), readI32())

  override def readMapEnd(): Unit = ()

  override def readListBegin(): TList = new TList(readByte(), readI32())

  override def readListEnd(): Unit = ()

  override def readSetBegin(): TSet = new TSet(readByte(), readI32())

  override def readSetEnd(): Unit = ()

  override def readBool(): Boolean = readByte() == 1

  @inline
  override def readByte(): Byte = {
    val r: Byte = transport.srcBuf(transport.getBufferPosition)
    transport.advance(1)
    r
  }

  @inline
  override def readI32(): Int = {
    val off = transport.getBufferPosition
    transport.advance(4)
    decodeI32(transport.srcBuf, off)
  }

  @inline
  override def readI16(): Short = {
    val off = transport.getBufferPosition
    transport.advance(2)
    (((transport.srcBuf(off) & 0xff) << 8) | ((transport.srcBuf(off + 1) & 0xff))).toShort
  }

  @inline
  override def readI64(): Long = {
    val off = transport.getBufferPosition
    transport.advance(8)
    decodeI64(transport.srcBuf, off)
  }

  @inline
  override def readDouble(): Double =
    java.lang.Double.longBitsToDouble(readI64())

  private[this] def checkReadLength(length: Int): Unit = {
    if (length < 0) {
      throw new TException(s"Negative length: $length")
    }
    if (transport.getBytesRemainingInBuffer < length) {
      throw new TException(s"Message length exceeded: $length")
    }
  }

  override def readString(): String =
    try {
      val size = readI32()
      checkReadLength(size)
      val s = new String(transport.srcBuf, transport.getBufferPosition, size, utf8Charset)
      transport.advance(size)
      s
    } catch {
      case e: UnsupportedEncodingException =>
        throw new TException("JVM DOES NOT SUPPORT UTF-8")
    }

  override def buffer: Array[Byte] = transport.srcBuf

  override def offset: Int = transport.getBufferPosition

  override def decodeBool(buf: Array[Byte], offset: Int): Boolean =
    buf(offset) == 1

  override def decodeByte(buf: Array[Byte], offset: Int): Byte =
    buf(offset)

  override def decodeI16(buf: Array[Byte], off: Int): Short =
    (((buf(off) & 0xff) << 8) | ((buf(off + 1) & 0xff))).toShort

  override def decodeI32(buf: Array[Byte], off: Int): Int =
    ((buf(off) & 0xff) << 24) |
      ((buf(off + 1) & 0xff) << 16) |
      ((buf(off + 2) & 0xff) << 8) |
      ((buf(off + 3) & 0xff))

  override def decodeI64(buf: Array[Byte], off: Int): Long =
    ((buf(off) & 0xffL) << 56) |
      ((buf(off + 1) & 0xffL) << 48) |
      ((buf(off + 2) & 0xffL) << 40) |
      ((buf(off + 3) & 0xffL) << 32) |
      ((buf(off + 4) & 0xffL) << 24) |
      ((buf(off + 5) & 0xffL) << 16) |
      ((buf(off + 6) & 0xffL) << 8) |
      ((buf(off + 7) & 0xffL))

  override def decodeDouble(buf: Array[Byte], off: Int): Double =
    java.lang.Double.longBitsToDouble(decodeI64(buf, off))

  override def decodeString(buf: Array[Byte], off: Int): String =
    try {
      val size = decodeI32(buf, off)
      new String(buf, off + 4, size, utf8Charset)
    } catch {
      case e: StringIndexOutOfBoundsException =>
        throw new TException(
          s"Data is corrupt, string size reported as ${decodeI32(buf, off)}, array size is : ${buf.size} , with offset as $off"
          )
      case e: UnsupportedEncodingException =>
        throw new TException("JVM DOES NOT SUPPORT UTF-8")
    }

  override def offsetSkipBool(): Int = offsetSkipByte

  override def offsetSkipByte(): Int = {
    val pos = transport.getBufferPosition
    transport.advance(1)
    pos
  }

  override def offsetSkipI16(): Int = {
    val pos = transport.getBufferPosition
    transport.advance(2)
    pos
  }

  override def offsetSkipI32(): Int = {
    val pos = transport.getBufferPosition
    transport.advance(4)
    pos
  }

  override def offsetSkipI64(): Int = {
    val pos = transport.getBufferPosition
    transport.advance(8)
    pos
  }

  override def offsetSkipDouble(): Int = offsetSkipI64

  override def offsetSkipString(): Int = {
    val pos = transport.getBufferPosition
    val size = readI32()
    checkReadLength(size)
    transport.advance(size)
    pos
  }

  override def offsetSkipBinary(): Int = offsetSkipString

  override def readBinary(): ByteBuffer = {
    val size = readI32()
    checkReadLength(size)
    val bb = ByteBuffer.wrap(transport.srcBuf, transport.getBufferPosition, size)
    transport.advance(size)
    bb
  }

}