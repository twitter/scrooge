package com.twitter.scrooge

import org.apache.thrift.protocol.TProtocol

trait ThriftStruct {
  def write(oprot: TProtocol)
}

trait ThriftStructCodec[T <: ThriftStruct] {
  def encoder: (T, TProtocol) => Unit
  def decoder: (TProtocol) => T
}
