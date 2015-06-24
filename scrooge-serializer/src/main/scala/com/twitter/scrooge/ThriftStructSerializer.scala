package com.twitter.scrooge

import com.twitter.app.GlobalFlag
import com.twitter.util.{Base64StringEncoder, StringEncoder}
import java.io.{ByteArrayInputStream, InputStream}
import java.util.concurrent.atomic.AtomicLong
import org.apache.thrift.protocol.{
  TBinaryProtocol, TCompactProtocol, TProtocolFactory, TSimpleJSONProtocol}
import org.apache.thrift.transport.TIOStreamTransport


object maxReusableBufferSize extends GlobalFlag[Int](
  16 * 1024,
  "Max bytes for ThriftStructSerializers reusable transport buffer")
{
  override val name = "com.twitter.scrooge.ThriftStructSerializer.maxReusableBufferSize"
}

private object ThriftStructSerializer {

  val transportTooBig = new AtomicLong(0)

  val reusableTransport = new ThreadLocal[TReusableMemoryTransport] {
    override def initialValue(): TReusableMemoryTransport =
      TReusableMemoryTransport(maxReusableBufferSize())
  }

}

trait ThriftStructSerializer[T <: ThriftStruct] {
  import ThriftStructSerializer._

  def codec: ThriftStructCodec[T]
  def protocolFactory: TProtocolFactory
  def encoder: StringEncoder = Base64StringEncoder

  def toBytes(obj: T): Array[Byte] = {
    val trans = reusableTransport.get()
    try {
      val proto = protocolFactory.getProtocol(trans)
      codec.encode(obj, proto)
      val bytes = new Array[Byte](trans.length())
      trans.read(bytes, 0, trans.length())
      bytes
    } finally {
      if (trans.currentCapacity > maxReusableBufferSize()) {
        transportTooBig.incrementAndGet()
        reusableTransport.remove()
      } else {
        trans.reset()
      }
    }
  }

  def fromBytes(bytes: Array[Byte]): T = {
    fromInputStream(new ByteArrayInputStream(bytes))
  }

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

object LazyBinaryThriftStructSerializer {
  private val reusuableProtocolAndTransport = new ThreadLocal[(TArrayByteTransport, TLazyBinaryProtocol)] {
    override def initialValue(): (TArrayByteTransport, TLazyBinaryProtocol) = {
      val transport = new TArrayByteTransport
      val proto = new TLazyBinaryProtocol(transport)
      (transport, proto)
    }
  }

  def apply[T <: ThriftStruct](_codec: ThriftStructCodec[T]): LazyBinaryThriftStructSerializer[T] =
    new LazyBinaryThriftStructSerializer[T] {
      def codec = _codec
    }
}

trait LazyBinaryThriftStructSerializer[T <: ThriftStruct] extends ThriftStructSerializer[T] {
  import LazyBinaryThriftStructSerializer._

  // Since we only support the fast path reading from the TArrayByteTransport
  // we provide the default if someone hits it to be the TBinaryProtocol which we are wire compatible with.
  override val protocolFactory = new TBinaryProtocol.Factory

  override def toBytes(obj: T): Array[Byte] = {
    val (transport, proto) = reusuableProtocolAndTransport.get()
    transport.reset
    codec.encode(obj, proto)
    transport.toByteArray
  }

  override def fromBytes(bytes: Array[Byte]): T = {
    val (transport, proto) = reusuableProtocolAndTransport.get()
    transport.setBytes(bytes)
    codec.decode(proto)
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
