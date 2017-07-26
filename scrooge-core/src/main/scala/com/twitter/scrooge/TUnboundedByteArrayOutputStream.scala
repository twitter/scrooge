package com.twitter.scrooge

import org.apache.thrift.TByteArrayOutputStream

/**
 * A version of ByteArrayOutputStream that exposes the internal buffer.
 * It's used to replace TByteArrayOutputStream so that the internal buffer won't be reallocated
 * on reset.
 */
private[scrooge] class TUnboundedByteArrayOutputStream(size: Int)
    extends TByteArrayOutputStream(size) {
  override def reset(): Unit = {
    count = 0
  }
}
