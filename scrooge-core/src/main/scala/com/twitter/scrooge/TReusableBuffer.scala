package com.twitter.scrooge

/**
 * This is a per-thread managed resource and must be reset after use
 * {{{
 *   import com.twitter.scrooge.TReusableBuffer
 *
 *   class Example {
 *     private[this] val reusableBuffer = new TReusableBuffer()
 *
 *     def someMethod(): Unit = {
 *       val buffer = reusableBuffer.get()
 *       try {
 *         // code that uses buffer
 *       } finally {
 *         buffer.reset()
 *       }
 *     }
 *   }
 * }}}
 *
 * @param initialSize The initial buffer size, default is 512.
 * @param maxThriftBufferSize The buffer will reset if it exceeds max buffer
 *                            size, default is 16K.
 */
case class TReusableBuffer(initialSize: Int = 512, maxThriftBufferSize: Int = 16 * 1024) {

  private[this] val tlReusableBuffer = new ThreadLocal[TReusableMemoryTransport] {
    override def initialValue(): TReusableMemoryTransport = TReusableMemoryTransport(initialSize)
  }

  /**
   * NOTE: This method resets the underlying TReusableMemoryTransport before returning it.
   */
  def get(): TReusableMemoryTransport = {
    val buf = tlReusableBuffer.get()
    buf.reset()
    buf
  }

  def reset(): Unit = {
    if (tlReusableBuffer.get().currentCapacity > maxThriftBufferSize) {
      tlReusableBuffer.remove()
    }
  }
}
