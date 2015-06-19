package com.twitter.scrooge

import org.apache.thrift.protocol._

/**
 * An extension to the TProtocol to enable lazy reading
 *
 * Three main classes of operations are intended to be enabled here:
 *
 * 1) Enable caching of a backing Array[Byte] so we can serialize quickly what we just deserialized if unchanged.
 * 2) Enabling deferred string decoding, since string decoding is an expensive operation if we don't need the string
 *    large savings can be had avoiding this for all strings in our deserialization path.
 * 3) Optional fields require boxing + allocations during deserialization for primitive types, this stores the offset to those
 *    types instead, doing a lazy instantiation of the Option when the field is first accessed.
 */
trait LazyTProtocol extends TProtocol {

  /**
   * Take a segment of an Array[Byte] and presume it is already
   * encoded for this transport and just copy it down.
   *
   * This is useful if we have a cached set of bytes used when
   * deserializing a field or struct
   */
  def writeRaw(buf: Array[Byte], offset: Int, len: Int): Unit

  /**
   * Return the underlying Array[Byte] used in reading
   */
  def buffer: Array[Byte]

  /**
   * Return the offset currently on the underlying array byte of the transport used in reading.
   */
  def offset: Int

  /**
   * Given a backing Array[Byte] and offset, decode this type from it
   * using this TProtocol's deserializer
   */
  def decodeBool(arr: Array[Byte], offset: Int): Boolean

  /**
   * Given a backing Array[Byte] and offset, decode this type from it
   * using this TProtocol's deserializer
   */
  def decodeByte(arr: Array[Byte], offset: Int): Byte

  /**
   * Given a backing Array[Byte] and offset, decode this type from it
   * using this TProtocol's deserializer
   */
  def decodeI16(arr: Array[Byte], offset: Int): Short

  /**
   * Given a backing Array[Byte] and offset, decode this type from it
   * using this TProtocol's deserializer
   */
  def decodeI32(arr: Array[Byte], offset: Int): Int

  /**
   * Given a backing Array[Byte] and offset, decode this type from it
   * using this TProtocol's deserializer
   */
  def decodeI64(arr: Array[Byte], offset: Int): Long

  /**
   * Given a backing Array[Byte] and offset, decode this type from it
   * using this TProtocol's deserializer
   */
  def decodeDouble(arr: Array[Byte], offset: Int): Double

  /**
   * Given a backing Array[Byte] and offset, decode this type from it
   * using this TProtocol's deserializer
   */
  def decodeString(arr: Array[Byte], offset: Int): String

  /**
   * Skips the length of a boolean on the underlying transport
   * Returns: The offset at which the boolean can be read.
   */
  def offsetSkipBool(): Int

  /**
   * Skips the length of a byte on the underlying transport
   * Returns: The offset at which the byte can be read.
   */
  def offsetSkipByte(): Int

  /**
   * Skips the length of a short on the underlying transport
   * Returns: The offset at which the short can be read.
   */
  def offsetSkipI16(): Int

  /**
   * Skips the length of a int on the underlying transport
   * Returns: The offset at which the int can be read.
   */
  def offsetSkipI32(): Int

  /**
   * Skips the length of a long on the underlying transport
   * Returns: The offset at which the long can be read.
   */
  def offsetSkipI64(): Int

  /**
   * Skips the length of a double on the underlying transport
   * Returns: The offset at which the double can be read.
   */
  def offsetSkipDouble(): Int

  /**
   * Skips the length of a string on the underlying transport
   * Returns: The offset at which the string can be read.
   */
  def offsetSkipString(): Int

  /**
   * Skips the length of a string on the underlying transport
   * Returns: The offset at which the string can be read.
   */
  def offsetSkipBinary(): Int

}
