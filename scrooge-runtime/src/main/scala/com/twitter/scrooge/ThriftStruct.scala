package com.twitter.scrooge

import org.apache.thrift.protocol.TProtocol

trait ThriftStruct {
  @throws(classOf[org.apache.thrift.TException])
  def write(oprot: TProtocol)
}

abstract class ThriftStructCodec[T <: ThriftStruct] {
  @throws(classOf[org.apache.thrift.TException])
  def encode(t: T, oprot: TProtocol)

  @throws(classOf[org.apache.thrift.TException])
  def decode(iprot: TProtocol): T

  lazy val metaData = new ThriftStructMetaData(this)

  @deprecated
  def encoder: (T, TProtocol) => Unit = encode _

  @deprecated
  def decoder: TProtocol => T = decode _
}
