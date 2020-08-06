package com.twitter.scrooge

/**
 * This trait extends from HasThriftStructCodec3 and ThriftStruct.
 * It should be safe to call "validatingStruct._codec.validateNewInstance(validatingStruct)"
 * on any validatingStruct that implements ValidatingThriftStruct. We take advantage of this fact in
 * the validateField method in ValidatingThriftStructCodec3.
 *
 * A method could be added to this trait that does this (with more type safety), but we want to
 * avoid adding unnecessary methods to thrift structs.
 */
trait ValidatingThriftStruct[T <: ThriftStruct] extends ThriftStruct with HasThriftStructCodec3[T] {
  self: T =>
  override def _codec: ValidatingThriftStructCodec3[T]
}
