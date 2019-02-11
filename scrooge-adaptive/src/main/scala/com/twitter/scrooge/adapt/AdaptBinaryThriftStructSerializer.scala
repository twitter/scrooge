package com.twitter.scrooge.adapt

import com.twitter.scrooge.{
  TArrayByteTransport,
  ThriftStruct,
  ThriftStructCodec,
  ThriftStructSerializer
}
import java.util
import org.apache.thrift.protocol.{TBinaryProtocol, TProtocolFactory}

/**
 * How adaptive scrooge does adaptations can be configured using the following:
 * @param trackedReads Number of thrift objects that are tracked before
 *                     triggering adaptation. Note that this count is updated
 *                     as soon as the thrift object is deserialized, fields
 *                     accesses happen later. Choose a (higher) value
 *                     accordingly to account for the delay.
 * @param useThreshold Number of times a field is accessed during the tracking
 *                     period for it to be considered used.
 */
case class AdaptSettings(trackedReads: Int, useThreshold: Int)

object AdaptBinaryThriftStructSerializer {
  type ProtocolAndTransport = (AdaptTProtocol, TArrayByteTransport)

  // Memoize protocol for setting
  private val reusableProtocolAndTransportMap =
    new ThreadLocal[util.HashMap[AdaptSettings, ProtocolAndTransport]] {
      override def initialValue(): util.HashMap[AdaptSettings, ProtocolAndTransport] =
        new util.HashMap[AdaptSettings, ProtocolAndTransport]()
    }

  /**
   * Thread local cache protocol for a setting.
   */
  private def cachedProtocol(settings: AdaptSettings): ProtocolAndTransport = {

    /*
     * The protocol is mutable but this is threadsafe because we have a
     * separate copy for each thread. This way we reuse the underlying
     * byte arrays and avoid gc cost.
     */
    val protoTransMap = reusableProtocolAndTransportMap.get()
    var protoTrans = protoTransMap.get(settings)
    if (protoTrans == null) {
      val transport = new TArrayByteTransport()
      val proto = new TAdaptBinaryProtocol(transport, new TrackingAdaptContext(settings))
      protoTrans = (proto, transport)
      protoTransMap.put(settings, protoTrans)
    }
    protoTrans
  }

  /**
   * Build an Adaptive binary thrift serializer from settings. Reuses
   * threadlocal transport for efficiency and concurrency safety.
   */
  def apply[T <: ThriftStruct](
    codec: ThriftStructCodec[T],
    settings: AdaptSettings
  ): ThriftStructSerializer[T] =
    new AdaptBinaryThriftStructSerializer[T](codec, settings)

  /**
   * A serializer for binary thrift that does adaptive decoding.
   * @see [[AdaptTProtocol]]
   */
  private[this] class AdaptBinaryThriftStructSerializer[T <: ThriftStruct](
    val codec: ThriftStructCodec[T],
    settings: AdaptSettings)
      extends ThriftStructSerializer[T] {

    // Since we only support the fast path reading from the TArrayByteTransport
    // we provide the default if someone hits it to be the TBinaryProtocol
    // which we are wire compatible with.
    val protocolFactory: TProtocolFactory = new TBinaryProtocol.Factory

    override def toBytes(obj: T): Array[Byte] = {
      val (proto, transport) = cachedProtocol(settings)
      transport.reset()
      codec.encode(obj, proto)
      transport.toByteArray
    }

    override def fromBytes(bytes: Array[Byte]): T = {
      val (proto, transport) = cachedProtocol(settings)
      transport.setBytes(bytes)
      codec.decode(proto)
    }
  }
}
