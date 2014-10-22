package com.twitter.scrooge

import org.apache.thrift.TException
import org.apache.thrift.protocol._
import org.apache.thrift.transport.TTransport

object ThriftUtil {
  val EmptyPassthroughs = Map.empty[TField, TTransport]

  def transfer(outProt: TProtocol, inProt: TProtocol, typ: Byte): Unit =
    transfer(outProt, inProt, typ, Int.MaxValue)

  /**
   * Transfers a piece of thrift data from one TProtocol to another.
   *
   * @outProt the protocol that the data will be written to
   * @inProt the protocol that the data will be read from
   * @typ specifies the type of thrift data to be read
   * @maxDepth specifies how deeply to recurse through the data transferring it
   */
  def transfer(outProt: TProtocol, inProt: TProtocol, typ: Byte, maxDepth: Int): Unit = {
    if (maxDepth <= 0)
      throw new TException("Maximum depth exceeded")

    typ match {
      case TType.BOOL =>
        outProt.writeBool(inProt.readBool())

      case TType.BYTE =>
        outProt.writeByte(inProt.readByte())

      case TType.I16 =>
        outProt.writeI16(inProt.readI16())

      case TType.I32 =>
        outProt.writeI32(inProt.readI32())

      case TType.I64 =>
        outProt.writeI64(inProt.readI64())

      case TType.DOUBLE =>
        outProt.writeDouble(inProt.readDouble())

      case TType.STRING =>
        outProt.writeBinary(inProt.readBinary())

      case TType.STRUCT =>
        val struct = inProt.readStructBegin()
        outProt.writeStructBegin(struct)
        var done = false
        while (!done) {
          val field = inProt.readFieldBegin()
          if (field.`type` == TType.STOP) {
            outProt.writeFieldStop()
            done = true
          } else {
            outProt.writeFieldBegin(field)
            transfer(outProt, inProt, field.`type`, maxDepth - 1)
            inProt.readFieldEnd()
            outProt.writeFieldEnd()
          }
        }
        inProt.readStructEnd()
        outProt.writeStructEnd()

      case TType.MAP =>
        val map = inProt.readMapBegin()
        outProt.writeMapBegin(map)
        (0 until map.size) foreach { _ =>
          transfer(outProt, inProt, map.keyType, maxDepth - 1)
          transfer(outProt, inProt, map.valueType, maxDepth - 1)
        }
        inProt.readMapEnd()
        outProt.writeMapEnd()

      case TType.SET =>
        val set = inProt.readSetBegin()
        outProt.writeSetBegin(set)
        (0 until set.size) foreach { _ =>
          transfer(outProt, inProt, set.elemType, maxDepth - 1)
        }
        inProt.readSetEnd()
        outProt.writeSetEnd()

      case TType.LIST =>
        val list = inProt.readListBegin()
        outProt.writeListBegin(list)
        (0 until list.size) foreach { _ =>
          transfer(outProt, inProt, list.elemType, maxDepth - 1)
        }
        inProt.readListEnd()
        outProt.writeListEnd()
    }
  }
}
