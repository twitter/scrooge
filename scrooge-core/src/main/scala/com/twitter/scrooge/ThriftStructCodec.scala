package com.twitter.scrooge

import org.apache.thrift.protocol.TProtocol

/**
 * A trait encapsulating the logic for encoding and decoding a specific thrift struct
 * type.
 */
trait ThriftStructCodec[T <: ThriftStruct] {
  @throws(classOf[org.apache.thrift.TException])
  def encode(t: T, oprot: TProtocol): Unit

  @throws(classOf[org.apache.thrift.TException])
  def decode(iprot: TProtocol): T

  lazy val metaData: ThriftStructMetaData[T] = ThriftStructMetaData(this)
}
