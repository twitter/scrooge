package com.twitter.scrooge

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import org.apache.thrift.protocol.{TBinaryProtocol, TCompactProtocol, TProtocol,
  TProtocolFactory, TSimpleJSONProtocol}
import org.apache.thrift.transport.TIOStreamTransport
import com.twitter.util.{Base64StringEncoder, StringEncoder}

trait ThriftStructSerializer[T <: ThriftStruct] {
  def codec: ThriftStructCodec[T]
  def protocolFactory: TProtocolFactory
  def encoder: StringEncoder = Base64StringEncoder

  def toBytes(obj: T): Array[Byte] = {
    val buf = new ByteArrayOutputStream
    val proto = protocolFactory.getProtocol(new TIOStreamTransport(buf))
    codec.encode(obj, proto)
    buf.toByteArray
  }

  def fromBytes(bytes: Array[Byte]): T = fromInputStream(new ByteArrayInputStream(bytes))

  def fromInputStream(stream: InputStream): T = {
    val proto = protocolFactory.getProtocol(new TIOStreamTransport(stream))
    codec.decode(proto)
  }

  def toString(obj: T): String = {
    encoder.encode(toBytes(obj))
  }

  def fromString(string: String): T = {
    fromBytes(encoder.decode(string))
  }
}

trait BinaryThriftStructSerializer[T <: ThriftStruct] extends ThriftStructSerializer[T] {
  val protocolFactory = new TBinaryProtocol.Factory
}

object BinaryThriftStructSerializer {
  def apply[T <: ThriftStruct](_codec: ThriftStructCodec[T]): BinaryThriftStructSerializer[T] =
    new BinaryThriftStructSerializer[T] {
      def codec = _codec
    }
}

trait CompactThriftSerializer[T <: ThriftStruct] extends ThriftStructSerializer[T] {
  val protocolFactory = new TCompactProtocol.Factory
}

object CompactThriftSerializer {
  def apply[T <: ThriftStruct](_codec: ThriftStructCodec[T]): CompactThriftSerializer[T] =
    new CompactThriftSerializer[T] {
      def codec = _codec
    }
}

trait JsonThriftSerializer[T <: ThriftStruct] extends ThriftStructSerializer[T] {
  override def encoder = StringEncoder
  val protocolFactory = new TSimpleJSONProtocol.Factory
}

object JsonThriftSerializer {
  def apply[T <: ThriftStruct](_codec: ThriftStructCodec[T]): JsonThriftSerializer[T] =
    new JsonThriftSerializer[T] {
      def codec = _codec
    }
}
