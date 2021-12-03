package com.twitter.scrooge.internal

import com.twitter.scrooge.TFieldBlob
import com.twitter.scrooge.ThriftEnum
import com.twitter.scrooge.ThriftUnion
import java.nio.ByteBuffer
import org.apache.thrift.protocol._
import scala.collection.immutable
import scala.collection.mutable

/**
 * Reads and writes fields for a `TProtocol`. Intended to be used
 * by generated code. See `StructTemplate`.
 *
 * This class is stateless and as such can be shared across
 * threads. As per usual with Thrift, the TProtocol instances
 * do have state and they are not thread-safe.
 *
 * @see [[TProtocols.apply()]] for an instance.
 *
 * @note this class, while public, is not intended to be
 *       used outside of generated code.
 */
final class TProtocols private[TProtocols] {

  def readSet[T](protocol: TProtocol, readElement: TProtocol => T): collection.Set[T] = {
    val tset: TSet = protocol.readSetBegin()
    if (tset.size == 0) {
      protocol.readSetEnd()
      Set.empty[T]
    } else {
      val set = new mutable.HashSet[T]()
      var i = 0
      do {
        val element = readElement(protocol)
        set += element
        i += 1
      } while (i < tset.size)
      protocol.readSetEnd()
      set
    }
  }

  def readList[T](protocol: TProtocol, readElement: TProtocol => T): Seq[T] = {
    val tlist: TList = protocol.readListBegin()
    if (tlist.size == 0) {
      protocol.readListEnd()
      Nil
    } else {
      val buff = new mutable.ArrayBuffer[T](tlist.size)
      var i = 0
      do {
        val element = readElement(protocol)
        buff += element
        i += 1
      } while (i < tlist.size)
      protocol.readListEnd()
      buff.toSeq
    }
  }

  def readMap[K, V](
    protocol: TProtocol,
    readKey: TProtocol => K,
    readValue: TProtocol => V
  ): collection.Map[K, V] = {
    val tmap = protocol.readMapBegin()
    if (tmap.size == 0) {
      protocol.readMapEnd()
      Map.empty[K, V]
    } else {
      val map = new mutable.HashMap[K, V]()
      var i = 0
      do {
        val key = readKey(protocol)
        val value = readValue(protocol)
        map(key) = value
        i += 1
      } while (i < tmap.size)
      protocol.readMapEnd()
      map
    }
  }

  /** ENUMs are written as I32s */
  private[this] def typeForCollection(elementType: Byte): Byte =
    if (elementType == TType.ENUM) TType.I32
    else elementType

  def writeList[T](
    protocol: TProtocol,
    list: collection.Seq[T],
    elementType: Byte,
    writeElement: (TProtocol, T) => Unit
  ): Unit = {
    protocol.writeListBegin(new TList(typeForCollection(elementType), list.size))
    list match {
      case _: IndexedSeq[_] =>
        var i = 0
        while (i < list.size) {
          writeElement(protocol, list(i))
          i += 1
        }
      case _ =>
        list.foreach { element =>
          writeElement(protocol, element)
        }
    }
    protocol.writeListEnd()
  }

  def writeSet[T](
    protocol: TProtocol,
    set: collection.Set[T],
    elementType: Byte,
    writeElement: (TProtocol, T) => Unit
  ): Unit = {
    protocol.writeSetBegin(new TSet(typeForCollection(elementType), set.size))
    set.foreach { element =>
      writeElement(protocol, element)
    }
    protocol.writeSetEnd()
  }

  def writeMap[K, V](
    protocol: TProtocol,
    map: collection.Map[K, V],
    keyType: Byte,
    writeKey: (TProtocol, K) => Unit,
    valueType: Byte,
    writeValue: (TProtocol, V) => Unit
  ): Unit = {
    protocol.writeMapBegin(
      new TMap(typeForCollection(keyType), typeForCollection(valueType), map.size))
    map.foreach {
      case (key, value) =>
        writeKey(protocol, key)
        writeValue(protocol, value)
    }
    protocol.writeMapEnd()
  }
}

object TProtocols {

  val NoPassthroughFields: immutable.Map[Short, TFieldBlob] =
    immutable.Map.empty[Short, TFieldBlob]

  /** Function1 for reading a Boolean from a TProtocol */
  val readBoolFn: TProtocol => Boolean =
    protocol => protocol.readBool()

  /** Function1 for reading a Byte from a TProtocol */
  val readByteFn: TProtocol => Byte =
    protocol => protocol.readByte()

  /** Function1 for reading a Short from a TProtocol */
  val readI16Fn: TProtocol => Short =
    protocol => protocol.readI16()

  /** Function1 for reading an Int from a TProtocol */
  val readI32Fn: TProtocol => Int =
    protocol => protocol.readI32()

  /** Function1 for reading a Long from a TProtocol */
  val readI64Fn: TProtocol => Long =
    protocol => protocol.readI64()

  /** Function1 for reading a Double from a TProtocol */
  val readDoubleFn: TProtocol => Double =
    protocol => protocol.readDouble()

  /** Function1 for reading a String from a TProtocol */
  val readStringFn: TProtocol => String =
    protocol => protocol.readString()

  /** Function1 for reading a ByteBuffer from a TProtocol */
  val readBinaryFn: TProtocol => ByteBuffer =
    protocol => protocol.readBinary()

  val writeBoolFn: (TProtocol, Boolean) => Unit =
    (protocol, value) => protocol.writeBool(value)

  val writeByteFn: (TProtocol, Byte) => Unit =
    (protocol, value) => protocol.writeByte(value)

  val writeI16Fn: (TProtocol, Short) => Unit =
    (protocol, value) => protocol.writeI16(value)

  val writeI32Fn: (TProtocol, Int) => Unit =
    (protocol, value) => protocol.writeI32(value)

  val writeI64Fn: (TProtocol, Long) => Unit =
    (protocol, value) => protocol.writeI64(value)

  val writeDoubleFn: (TProtocol, Double) => Unit =
    (protocol, value) => protocol.writeDouble(value)

  val writeStringFn: (TProtocol, String) => Unit =
    (protocol, value) => protocol.writeString(value)

  val writeBinaryFn: (TProtocol, ByteBuffer) => Unit =
    (protocol, value) => protocol.writeBinary(value)

  val writeEnumFn: (TProtocol, ThriftEnum) => Unit =
    (protocol, value) => protocol.writeI32(value.value)

  private[this] val instance: TProtocols = new TProtocols()

  /** An instance of a [[TProtocols]]. */
  def apply(): TProtocols = instance

  def validateFieldType(expected: Byte, actual: Byte, fieldName: String): Unit = {
    if (expected != actual) {
      ApplicationExceptions.throwWrongFieldTypeException(
        s"Received wrong type for field '$fieldName' (expected=%s, actual=%s).",
        expected,
        actual
      )
    }
  }

  def validateEnumFieldType(actual: Byte, fieldName: String): Unit = {
    if (!(actual == TType.ENUM || actual == TType.I32)) {
      ApplicationExceptions.throwWrongFieldTypeException(
        s"Received wrong type for field '$fieldName' (expected=%s, actual=%s).",
        TType.ENUM,
        actual
      )
    }
  }

  def throwMissingRequiredField(structName: String, fieldName: String): Unit = {
    throw new TProtocolException(
      s"Required field '$fieldName' was not found in serialized data for struct $structName")
  }

  /**
   * Reads the next passthrough field into a [[TFieldBlob]]
   * and returns it in a mutable Map Builder, keyed by the
   * [[TField]]'s id.
   *
   * Note: `passthroughs` may be `null`. If so, a new Builder
   * will be returned. Otherwise, the Builder will be mutated.
   */
  def readPassthroughField(
    protocol: TProtocol,
    field: TField,
    passthroughs: mutable.Builder[(Short, TFieldBlob), immutable.Map[Short, TFieldBlob]]
  ): mutable.Builder[(Short, TFieldBlob), immutable.Map[Short, TFieldBlob]] = {
    val builder =
      if (passthroughs eq null) immutable.Map.newBuilder[Short, TFieldBlob]
      else passthroughs
    val value = TFieldBlob.read(field, protocol)
    builder += field.id -> value
  }

  /**
   * Note: `result` may be `null`.
   *
   * Note: there is no corresponding `startReadingUnion` method as
   * there was no benefit today to factoring that part out.
   */
  def finishReadingUnion(protocol: TProtocol, fieldType: Byte, result: ThriftUnion): Unit = {
    if (fieldType != TType.STOP) {
      protocol.readFieldEnd()
      var done = false
      var moreThanOne = false
      do {
        val nextField = protocol.readFieldBegin()
        if (nextField.`type` == TType.STOP) {
          done = true
        } else {
          moreThanOne = true
          TProtocolUtil.skip(protocol, nextField.`type`)
          protocol.readFieldEnd()
        }
      } while (!done)
      if (moreThanOne) {
        protocol.readStructEnd()
        throw new TProtocolException("Cannot read a TUnion with more than one set value!")
      }
    }
    protocol.readStructEnd()
    if (result eq null)
      throw new TProtocolException("Cannot read a TUnion with no set value!")
  }

  /**
   * Note: there is no corresponding `startWritingStruct` method as
   * there was no benefit today to factoring that part out.
   */
  def finishWritingStruct(
    protocol: TProtocol,
    passthroughs: immutable.Map[Short, TFieldBlob]
  ): Unit = {
    if (passthroughs.nonEmpty) {
      passthroughs.values.foreach(_.write(protocol))
    }
    protocol.writeFieldStop()
    protocol.writeStructEnd()
  }

}
