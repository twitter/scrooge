package com.twitter.scrooge.adapt.testutil

import com.twitter.scrooge.adapt.TAdaptBinaryProtocol
import org.apache.thrift.protocol.TType

/**
 * Remove field with an id from bytes of a thrift struct generated using Binary
 * protocol.
 * Mainly used for testing, e.g. creating invalid thrift objects that don't
 * have required fields set.
 */
object BinaryThriftFieldRemover {
  import TAdaptBinaryProtocol._

  def removeField(thriftBytes: Array[Byte], fieldToRemove: Short): Array[Byte] = {
    fieldRange(thriftBytes, fieldToRemove) match {
      case Some((start, end)) =>
        val l = end - start
        val pruned = new Array[Byte](thriftBytes.size - l)
        System.arraycopy(thriftBytes, 0, pruned, 0, start)
        System.arraycopy(thriftBytes, end, pruned, start, pruned.size - start)
        pruned
      case None => thriftBytes
    }
  }

  private[this] def decodeI32(buf: Array[Byte], off: Int): Int =
    ((buf(off) & 0xff) << 24) |
      ((buf(off + 1) & 0xff) << 16) |
      ((buf(off + 2) & 0xff) << 8) |
      ((buf(off + 3) & 0xff))

  private[this] def skipStruct(ba: Array[Byte], pos: Int): Int = {
    var p = pos
    var done = false
    while (!done) {
      val tpe: Byte = ba(p)
      if (tpe == TType.STOP) {
        p += FieldTypeBytes
        done = true
      } else {
        p += FieldTypeAndIdBytes
        p = skipValue(tpe, ba, p)
      }
    }
    p
  }

  private[this] def skipList(ba: Array[Byte], pos: Int): Int = {
    var p = pos
    val elemType = ba(p)
    val size = decodeI32(ba, p + 1)
    p += I32WithTypeBytes
    var i = 0
    while (i < size) {
      p = skipValue(elemType, ba, p)
      i += 1
    }
    p
  }

  private[this] def skipSet(ba: Array[Byte], pos: Int): Int = skipList(ba, pos)

  private[this] def skipMap(ba: Array[Byte], pos: Int): Int = {
    var p = pos
    val keyType: Byte = ba(p)
    val valueType: Byte = ba(p + FieldTypeBytes)
    val size = decodeI32(ba, p + TwoFieldTypesBytes)
    p += I32WithTwoFieldTypesBytes

    var i = 0
    while (i < size) {
      p = skipValue(keyType, ba, p)
      p = skipValue(valueType, ba, p)
      i += 1
    }
    p
  }

  private[this] def skipString(ba: Array[Byte], pos: Int): Int = {
    val size = decodeI32(ba, pos)
    pos + size + I32Bytes
  }

  private[this] def skipValue(tpe: Byte, ba: Array[Byte], pos: Int): Int =
    tpe match {
      case TType.STRUCT => skipStruct(ba, pos)
      case TType.I64 => pos + I64Bytes
      case TType.I32 => pos + I32Bytes
      case TType.I16 => pos + I16Bytes
      case TType.BOOL => pos + BoolBytes
      case TType.BYTE => pos + ByteBytes
      case TType.DOUBLE => pos + DoubleBytes
      case TType.VOID => pos
      case TType.STRING => skipString(ba, pos)
      case TType.ENUM => pos + EnumBytes
      case TType.LIST => skipList(ba, pos)
      case TType.MAP => skipMap(ba, pos)
      case TType.SET => skipSet(ba, pos)
    }

  private[this] def readFieldId(ba: Array[Byte], pos: Int) =
    (((ba(pos) & 0xff) << 8) | ((ba(pos + 1) & 0xff))).toShort

  private[this] def fieldRange(ba: Array[Byte], fieldToRemove: Short): Option[(Int, Int)] = {
    var start = 0
    var end = 0
    var range = Option.empty[(Int, Int)]
    var done = false
    var pos = 0
    while (!done) {
      val tpe: Byte = ba(pos)
      pos += FieldTypeBytes
      if (tpe == TType.STOP) {
        done = true
      } else {
        val fieldId = readFieldId(ba, pos)
        pos += FieldIdBytes
        if (fieldId == fieldToRemove) {
          start = pos - FieldTypeAndIdBytes
        }
        pos = skipValue(tpe, ba, pos)
        if (fieldId == fieldToRemove) {
          end = pos
          range = Some((start, end))
          done = true
        }
      }
    }
    return range
  }

}
