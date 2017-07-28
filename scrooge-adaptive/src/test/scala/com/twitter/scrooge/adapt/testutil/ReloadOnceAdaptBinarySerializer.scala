package com.twitter.scrooge.adapt.testutil

import com.twitter.scrooge.adapt.{AdaptSettings, TAdaptBinaryProtocol, TrackingAdaptContext}
import com.twitter.scrooge.{
  TArrayByteTransport,
  ThriftStruct,
  ThriftStructCodec,
  ThriftStructSerializer
}
import org.apache.thrift.protocol.{TBinaryProtocol, TProtocolFactory}

object ReloadOnceAdaptBinarySerializer {

  /**
   * Build an Adaptive binary thrift serializer that triggers decoder reload the
   * first time it's used.
   * This is useful for testing, when we want to trigger adaptation for
   * each test.
   */
  def apply[T <: ThriftStruct](
    codec: ThriftStructCodec[T],
    settings: AdaptSettings = AdaptSettings(1, 1)
  ): ThriftStructSerializer[T] =
    new ReloadOnceAdaptBinarySerializer[T](codec, settings)

  private[this] class ReloadOnceAdaptBinarySerializer[T <: ThriftStruct](
    val codec: ThriftStructCodec[T],
    settings: AdaptSettings
  ) extends ThriftStructSerializer[T] {

    private[this] val adaptContext = new ReloadOnceAdaptContext(new TrackingAdaptContext(settings))

    val protocolFactory: TProtocolFactory = new TBinaryProtocol.Factory

    override def toBytes(obj: T): Array[Byte] = {
      val transport = new TArrayByteTransport()
      val proto = new TAdaptBinaryProtocol(transport, adaptContext)
      transport.reset()
      codec.encode(obj, proto)
      transport.toByteArray
    }

    override def fromBytes(bytes: Array[Byte]): T = {
      val transport = new TArrayByteTransport()
      val proto = new TAdaptBinaryProtocol(transport, adaptContext)
      transport.setBytes(bytes)
      codec.decode(proto)
    }
  }
}
