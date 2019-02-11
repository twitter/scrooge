package com.twitter.scrooge.adapt

import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec}

/**
 * An AdaptContext that builds a decoder that tracks field accesses
 * to do adaptive decoding.
 * @see `AdaptTProtocol`
 * @param settings        Settings that govern how adaptation is done
 */
class TrackingAdaptContext(settings: AdaptSettings) extends AdaptContext {
  private[this] val adaptClassLoader =
    new AdaptClassLoader(this.getClass.getClassLoader)

  def buildDecoder[T <: ThriftStruct](
    codec: ThriftStructCodec[T],
    fallbackDecoder: Decoder[T],
    accessRecordingDecoderBuilder: AccessRecorder => Decoder[T]
  ): Decoder[T] = {
    new AdaptTrackingDecoder[T](
      codec,
      fallbackDecoder,
      accessRecordingDecoderBuilder,
      settings,
      adaptClassLoader
    )
  }

  def shouldReloadDecoder: Boolean = false

  def initCopy(): AdaptContext = new TrackingAdaptContext(settings)
}
