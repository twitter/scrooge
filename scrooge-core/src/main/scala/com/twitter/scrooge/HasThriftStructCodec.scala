package com.twitter.scrooge

trait HasThriftStructCodec3[T <: ThriftStruct] {
  def _codec: ThriftStructCodec3[T]
}
