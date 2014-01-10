package com.twitter.scrooge

import org.apache.thrift.transport.TTransport
import org.apache.thrift.TByteArrayOutputStream

object TReusableMemoryTransport {

  def apply(initialSize: Int = 512): TReusableMemoryTransport = {
    new TReusableMemoryTransport(new TByteArrayOutputStream(initialSize))
  }

}

/**
 * A version of TMemoryTransport that allows for reuse in order to minimize
 * object allocations.
 */
class TReusableMemoryTransport(
    baos: TByteArrayOutputStream)
  extends TTransport
{

  private[this] var readPos = 0

  /**
   * Resets both reads and writes.
   */
  def reset() {
    baos.reset()
    readPos = 0
  }

  def numWrittenBytes: Int = baos.len()

  // Here for drop-in api compatibility with TMemoryBuffer
  def length(): Int = numWrittenBytes

  // Here for drop-in api compatibility with TMemoryBuffer
  def getArray(): Array[Byte] = baos.get()

  /**
   * Total bytes currently allowed in the struct.
   *
   * Note: more writes beyond this length will cause the struct to grow.
   */
  def currentCapacity: Int = baos.get().length

  override def isOpen: Boolean = true

  override def close() { }

  override def open() { }

  override def write(from: Array[Byte], off: Int, len: Int) {
    baos.write(from, off, len)
  }

  override def read(into: Array[Byte], off: Int, len: Int): Int = {
    val bytesToRead = if (len > baos.len() - readPos) {
      baos.len() - readPos
    } else {
      len
    }
    if (bytesToRead > 0) {
      System.arraycopy(baos.get(), readPos, into, off, bytesToRead)
      readPos += bytesToRead
    }
    bytesToRead
  }

}
