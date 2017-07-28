package com.twitter.scrooge.adapt.testutil

import com.twitter.scrooge.adapt.{AccessRecorder, AdaptContext, Decoder}
import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec}
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Wraps an adapt context to provide reload once behavior i.e. it when
 * shouldReloadDecoder is called the first time then and only then true is
 * returned. This is useful for testing where we want a new decoder to be
 * created for each test but wouldn't want reload to be triggered every time
 * inside the test.
 */
class ReloadOnceAdaptContext(underlying: AdaptContext) extends AdaptContext {
  private[this] val reloaded = new AtomicBoolean
  def buildDecoder[T <: ThriftStruct](
    codec: ThriftStructCodec[T],
    fallbackDecoder: Decoder[T],
    accessRecordingDecoderBuilder: AccessRecorder => Decoder[T]
  ): Decoder[T] = underlying.buildDecoder(codec, fallbackDecoder, accessRecordingDecoderBuilder)

  def shouldReloadDecoder: Boolean =
    if (reloaded.get()) false
    else reloaded.compareAndSet(false, true)

  // initCopy is used for decoding thrift from bytes when
  // unused field is accessed. We don't want to reload
  // decoder in that case.
  def initCopy(): AdaptContext = underlying.initCopy()
}
