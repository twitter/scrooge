package com.twitter.scrooge.adapt

import com.twitter.scrooge.{TArrayByteTransport, TLazyBinaryProtocol}
import org.apache.thrift.protocol._

object TAdaptBinaryProtocol {
  // Constants for Thrift Binary protocol
  val FieldTypeBytes = 1
  val TwoFieldTypesBytes = 2 * FieldTypeBytes
  val FieldIdBytes = 2
  val FieldTypeAndIdBytes = FieldTypeBytes + FieldIdBytes
  val BoolBytes = 1
  val ByteBytes = 1
  val I16Bytes = 2
  val I32Bytes = 4
  val I32WithTypeBytes = I32Bytes + FieldTypeBytes
  val I32WithTwoFieldTypesBytes = I32Bytes + TwoFieldTypesBytes
  val EnumBytes = 4
  val I64Bytes = 8
  val DoubleBytes = 8
}

/**
 * This is an implementation of the AdaptTProtocol trait in scrooge-core.
 * Lot of functionality is borrowed from TLazyBinaryProtocol. Adds helper
 * methods to skip struct and collections. Also provides methods to help
 * with adaptive decoding. See doc comments for specific methods in base
 * trait to learn more.
 */
class TAdaptBinaryProtocol(
    transport: TArrayByteTransport,
    context: AdaptContext
) extends TLazyBinaryProtocol(transport) with AdaptTProtocol {
  import TAdaptBinaryProtocol._

  def adaptContext: AdaptContext = context

  def withBytes(bytes: Array[Byte]): AdaptTProtocol = {
    val trans = new TArrayByteTransport()
    trans.setBytes(bytes)
    new TAdaptBinaryProtocol(trans, context.initCopy())
  }

  def offsetSkipStruct(): Int = {
    val pos = transport.getBufferPosition
    var done = false
    while (!done) {
      val tpe: Byte = transport.srcBuf(transport.getBufferPosition)
      if (tpe == TType.STOP) {
        done = true
        transport.advance(FieldTypeBytes)
      } else {
        transport.advance(FieldTypeAndIdBytes)
        offsetSkipValue(tpe)
      }
    }
    pos
  }

  def offsetSkipList(): Int = {
    val pos = transport.getBufferPosition
    val elemType = transport.srcBuf(pos)
    // List is serialized as:
    // List Size | Values
    val size = decodeI32(transport.srcBuf, pos + FieldTypeBytes)
    transport.advance(I32WithTypeBytes)
    if (size == 0) {
      readListEnd()
    } else {
      var i = 0
      while (i < size) {
        offsetSkipValue(elemType)
        i += 1
      }
      readListEnd()
    }
    pos
  }

  def offsetSkipSet(): Int = offsetSkipList()

  def offsetSkipMap(): Int = {
    val pos = transport.getBufferPosition
    // Set is serialized as:
    // KeyType | ValueType | Keys | Values
    val keyType: Byte = transport.srcBuf(pos)
    val valueType: Byte = transport.srcBuf(pos + FieldTypeBytes)
    val size = decodeI32(transport.srcBuf, pos + TwoFieldTypesBytes)
    transport.advance(I32WithTwoFieldTypesBytes)
    if (size == 0) {
      readMapEnd()
    } else {
      var i = 0
      while (i < size) {
        offsetSkipValue(keyType)
        offsetSkipValue(valueType)
        i += 1
      }
      readMapEnd()
    }
    pos
  }

  def offsetSkipEnum(): Int = offsetSkipI32()

  private[this] def offsetSkipValue(tpe: Byte): Unit = tpe match {
    case TType.STRUCT => offsetSkipStruct()
    case TType.I64 => transport.advance(I64Bytes)
    case TType.I32 => transport.advance(I32Bytes)
    case TType.I16 => transport.advance(I16Bytes)
    case TType.BOOL => transport.advance(BoolBytes)
    case TType.BYTE => transport.advance(ByteBytes)
    case TType.DOUBLE => transport.advance(DoubleBytes)
    case TType.VOID =>
    case TType.STRING => offsetSkipString()
    case TType.ENUM => transport.advance(EnumBytes)
    case TType.LIST => offsetSkipList()
    case TType.MAP => offsetSkipMap()
    case TType.SET => offsetSkipSet()
  }
}
