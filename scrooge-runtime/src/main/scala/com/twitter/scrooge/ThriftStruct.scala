package com.twitter.scrooge

import org.apache.thrift.protocol.TProtocol

trait ThriftStruct {
  @throws(classOf[org.apache.thrift.TException])
  def write(oprot: TProtocol)
}

trait ThriftStructCodec[T <: ThriftStruct] {
  @throws(classOf[org.apache.thrift.TException])
  def encode(t: T, oprot: TProtocol)

  @throws(classOf[org.apache.thrift.TException])
  def decode(iprot: TProtocol): T
}
