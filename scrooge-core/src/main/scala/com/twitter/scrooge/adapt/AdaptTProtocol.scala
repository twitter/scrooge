package com.twitter.scrooge.adapt

import com.twitter.scrooge.{LazyTProtocol, ThriftStruct, ThriftStructCodec}
import org.apache.thrift.protocol.TProtocolException

trait AccessRecorder {
  def fieldAccessed(fieldId: Short): Unit
}

trait Decoder[T <: ThriftStruct] {
  def apply(protocol: AdaptTProtocol): T
}

/**
 * Helper methods for adaptive decoding. See comments for individual methods to
 * learn more.
 */
trait AdaptContext {

  /**
   * Build a decoder. This method is called by the scrooge generated code and
   * typically not by user written code.
   * @param fallback Scrooge generated code provides a decoder to fallback on in
   *                 case adaptive decoding is not considered useful.
   *                 e.g. if all fields in a thrift are accessed all the time.
   * @param accessRecordingDecoderBuilder Scrooge generated code provides a
   *                                      decoder that allows injecting an access
   *                                      recorder. The access recorder is called
   *                                      whenever any field of the thrift object
   *                                      is accessed. Knowledge of field
   *                                      accesses is used to build an adapted
   *                                      decoder.
   */
  def buildDecoder[T <: ThriftStruct](
    codec: ThriftStructCodec[T],
    fallback: Decoder[T],
    accessRecordingDecoderBuilder: AccessRecorder => Decoder[T]
  ): Decoder[T]

  /**
   * Should the caller reload the decoder.
   * This is a contract with the caller, which is honored by the scrooge
   * generated code. Ability to reload the decoder allows for testing. In future
   * this mechanism may be used to retrigger adaptation.
   */
  def shouldReloadDecoder: Boolean

  /**
   * Override this to provide a copy at initial state if implementation uses
   * mutable state. This is needed for Adaptive Scrooge to generate the protocol
   * for lazy decoding. When a field that is considered unused does get accessed
   * the entire thrift is deserialized again from original bytes. To do so a new
   * instance of protocol is needed and to create that AdaptContext is needed.
   */
  def initCopy(): AdaptContext
}

/**
 * These methods are used to mark code sections in Adaptive Decoder.
 * Based on usage of fields relevant sections are modified using ASM.
 */
object AdaptTProtocol {
  def usedStartMarker(fieldId: Short): Unit = ()
  def usedEndMarker(fieldId: Short): Unit = ()
  def unusedStartMarker(fieldId: Short): Unit = ()
  def unusedEndMarker(fieldId: Short): Unit = ()

  /**
   * Generate an exception to throw when unexpected type of
   * field is encountered during thrift parsing.
   */
  def unexpectedTypeException(
    expectedType: Byte,
    actualType: Byte,
    fieldName: String
  ): Throwable = {
    val expected = ThriftStruct.ttypeToString(expectedType)
    val actual = ThriftStruct.ttypeToString(actualType)
    new TProtocolException(
      s"Received wrong type for field '$fieldName' " +
        s"(expected=$expected, actual=$actual)."
    )
  }
}

/**
 * An extension to the TProtocol to enable adaptive reading. Learn from the
 * access pattern to skip unused fields and avoid creating them, thus making
 * parse faster and reduce GC.
 *
 * Also enable caching of a backing Array[Byte] so we can serialize quickly what
 * we just deserialized if unchanged.
 */
trait AdaptTProtocol extends LazyTProtocol {

  /**
   * Skip a struct. This still involves parsing but no objects are created.
   * @return The offset at which the struct can be read.
   */
  def offsetSkipStruct(): Int

  /**
   * Skip a list. This still involves parsing but no objects are created.
   * @return The offset at which the list can be read.
   */
  def offsetSkipList(): Int

  /**
   * Skip a set. This still involves parsing but no objects are created.
   * @return The offset at which the set can be read.
   */
  def offsetSkipSet(): Int

  /**
   * Skip a map. This still involves parsing but no objects are created.
   * @return The offset at which the map can be read.
   */
  def offsetSkipMap(): Int

  /**
   * Skip an Enum. This still involves parsing but no objects are created.
   */
  def offsetSkipEnum(): Int

  /**
   * Get a context that provides facilities for helping with Adaptive Scrooge
   * decoding.
   */
  def adaptContext: AdaptContext

  /**
   * Create a new protocol object set up to read from given bytes.
   */
  def withBytes(bytes: Array[Byte]): AdaptTProtocol

}
