package com.twitter.scrooge

/**
 * Introduced as a backwards compatible API bridge in Scrooge 3.
 * Scala generated structs extend from this class.
 *
 * @see [[ThriftStructCodec]]
 */
abstract class ThriftStructCodec3[T <: ThriftStruct] extends ThriftStructCodec[T] {
  protected def ttypeToString(byte: Byte): String = ThriftStruct.ttypeToString(byte)
}
