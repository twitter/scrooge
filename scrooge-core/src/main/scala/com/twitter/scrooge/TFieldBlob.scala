package com.twitter.scrooge

import java.util.Arrays
import org.apache.thrift.protocol._
import org.apache.thrift.transport.{TMemoryBuffer, TMemoryInputTransport}

object TFieldBlob {
  def read(field: TField, iprot: TProtocol): TFieldBlob = {
    capture(field) { ThriftUtil.transfer(_, iprot, field.`type`) }
  }

  def capture(field: TField)(f: TProtocol => Unit): TFieldBlob = {
    val buff = new TMemoryBuffer(32)
    val bprot = new TCompactProtocol(buff)
    f(bprot)
    val data = Arrays.copyOfRange(buff.getArray, 0, buff.length)
    TFieldBlob(field, data)
  }
}

/**
 * This class encapsulates a TField reference with a TCompactProtocol-encoded
 * binary blob.
 */
case class TFieldBlob(field: TField, data: Array[Byte]) {
  def id = field.id

  /**
   * Creates a TCompactProtocol to read the encoded data.
   */
  def read: TCompactProtocol =
    new TCompactProtocol(new TMemoryInputTransport(data))

  def write(oprot: TProtocol): Unit = {
    oprot.writeFieldBegin(field)
    ThriftUtil.transfer(oprot, read, field.`type`)
    oprot.writeFieldEnd()
  }

  /**
   * Niether TField nor Array[Byte] implement Object.equals(Object) in a way that is
   * appropriate, so we need a custom equals method to handle that.
   */
  override def equals(other: Any): Boolean =
    other match {
      // TField does not override the correct equals method, but instead implements
      // an equals method with a different signature, so we need to call .equals
      // instead of using ==
      case TFieldBlob(field2, data2) => field.equals(field2) && Arrays.equals(data, data2)
      case _ => false
    }
}
