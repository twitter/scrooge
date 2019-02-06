package com.twitter.scrooge

import com.twitter.io.Buf
import java.util.Arrays
import org.apache.thrift.protocol._
import org.apache.thrift.transport.{TMemoryBuffer, TMemoryInputTransport}

object TFieldBlob {

  def apply(field: TField, data: Array[Byte]): TFieldBlob =
    TFieldBlob(field, Buf.ByteArray.Owned(data))

  def read(field: TField, iprot: TProtocol): TFieldBlob = {
    capture(field) { ThriftUtil.transfer(_, iprot, field.`type`) }
  }

  def capture(field: TField)(f: TProtocol => Unit): TFieldBlob = {
    val buff = new TMemoryBuffer(32)
    val bprot = new TCompactProtocol(buff)
    f(bprot)
    TFieldBlob(field, Buf.ByteArray.Owned(buff.getArray()))
  }

  private val sysPropReadLength: Int =
    System.getProperty("org.apache.thrift.readLength", "-1").toInt
}

/**
 * This class encapsulates a TField reference with a TCompactProtocol-encoded
 * binary blob.
 */
case class TFieldBlob(field: TField, content: Buf) {
  import TFieldBlob._

  def this(field: TField, data: Array[Byte]) =
    this(field, Buf.ByteArray.Owned(data))

  def id = field.id

  @deprecated(
    "TFieldBlob now uses `c.t.io.Buf` to represent the data, call `content` instead",
    "2017-05-10"
  )
  lazy val data: Array[Byte] =
    Arrays.copyOfRange(Buf.ByteArray.Owned.extract(content), 0, content.length)

  /**
   * Creates a TCompactProtocol to read the encoded data.
   */
  def read: TCompactProtocol = {
    Buf.ByteArray.coerce(content) match {
      case Buf.ByteArray.Owned(bytes, off, len) =>
        new TCompactProtocol(new TMemoryInputTransport(bytes, off, len), sysPropReadLength, -1)
    }
  }

  def write(oprot: TProtocol): Unit = {
    oprot.writeFieldBegin(field)
    ThriftUtil.transfer(oprot, read, field.`type`)
    oprot.writeFieldEnd()
  }

  /**
   * Neither TField nor Array[Byte] implement Object.equals(Object) in a way that is
   * appropriate, so we need a custom equals method to handle that.
   */
  override def equals(other: Any): Boolean =
    other match {
      // TField does not override the correct equals method, but instead implements
      // an equals method with a different signature, so we need to call .equals
      // instead of using ==
      case TFieldBlob(field2, buf2) => field.equals(field2) && content.equals(buf2)
      case _ => false
    }
}
