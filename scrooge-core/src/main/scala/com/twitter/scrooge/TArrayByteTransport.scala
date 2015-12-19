package com.twitter.scrooge

import org.apache.thrift.protocol._
import org.apache.thrift.TException
import org.apache.thrift.transport.{TTransport, TTransportException}

/**
 * TArrayByteTransport decodes Array[Byte] to primitive types
 * This is a replacement transport optimized for Array[Byte]
 * and the TLazyBinaryProtocol
 *
 * NB. This class/transport is not thread safe, and contains mutable state.
 */
object TArrayByteTransport {
  def apply(buf: Array[Byte]): TArrayByteTransport = {
    val t = new TArrayByteTransport(0) // No write buffer used in read path
    t.setBytes(buf)
    t
  }
}

final class TArrayByteTransport(initialWriteBufferSize: Int = 512) extends TTransport {
  // Read state variables
  private[this] var bufferPos = 0
  private[this] var readbufferSiz_ = 0
  private[this] var srcBuf_ : Array[Byte] = null


  // Write state variables
  private[this] var writeBuffers: List[(Array[Byte], Int)] = Nil
  private[this] var totalSize = 0
  private[this] var nextBufferSize = initialWriteBufferSize

  private[this] var currentBuffer: Array[Byte] = Array.empty
  private[this] var currentOffset: Int = 0

  @inline private[this] def remainingSpaceInBuffer: Int = (currentBuffer.length - currentOffset)


  private[this] var writerOffset_ : Int = 0

  /*
   * Advance the internal buffer by the amount specified
   */
  @inline def advance(by: Int): Unit = {
    bufferPos += by
  }

  // Return the offset at which the last returned Array[Byte] should
  // be written to at
  @inline def writerOffset: Int = writerOffset_

  // Allow resetting the internal state down
  // this cache's the high water mark seen so far and keeps an internal buffer of that size however.
  def reset(): Unit = {
    if (currentBuffer != null) {
      if (currentBuffer.length < totalSize)
        currentBuffer = new Array(totalSize)
      currentOffset = 0
      writeBuffers = Nil
      writerOffset_ = 0
      nextBufferSize = currentBuffer.length * 2
      totalSize = 0
    }
  }

  /*
   * Offer an Array[Byte] to write to for the caller with enough space.
   * we update our writeOffset that the caller can then request too
   * This will allocate a new Array[Byte] if necessary. We keep track of all of their final
   * offsets so as we don't need to fill each of them.
   */
  def getBuffer(numBytes: Int): Array[Byte] = {
    totalSize += numBytes
    if (remainingSpaceInBuffer > numBytes) {
      writerOffset_ = currentOffset
      currentOffset += numBytes
      currentBuffer
    } else {
      if (currentBuffer != null)
        writeBuffers = (currentBuffer, currentOffset) :: writeBuffers
      nextBufferSize = if (nextBufferSize > numBytes * 2) nextBufferSize else numBytes * 2
      currentBuffer = new Array[Byte](nextBufferSize)
      currentOffset = numBytes
      nextBufferSize = nextBufferSize * 2
      writerOffset_ = 0
      currentBuffer
    }
  }


  override def write(buf: Array[Byte], off: Int, len: Int): Unit = {
    val dest = getBuffer(len)
    val destOffset = writerOffset
    System.arraycopy(buf, off, dest, destOffset, len)
  }

  /*
   * Take our internal state and present it as a byte array
   */
  def toByteArray: Array[Byte] = {
    (currentBuffer, writeBuffers) match {
      case (null, Nil) => new Array[Byte](0)
      case (buf, Nil) =>
        val finalBuf = new Array[Byte](totalSize)
        System.arraycopy(currentBuffer, 0, finalBuf, 0, totalSize)
        finalBuf
      case (buf, x) =>
        var reverseOffset = totalSize - currentOffset
        val finalBuf = new Array[Byte](totalSize)
        System.arraycopy(currentBuffer, 0, finalBuf, reverseOffset, currentOffset)

        writeBuffers.foreach {
          case (buf, siz) =>
            reverseOffset -= siz
            System.arraycopy(buf, 0, finalBuf, reverseOffset, siz)
        }
        finalBuf
    }
  }

  // Read methods from here:

  // Only used in reading to give a pointer to the Array[Byte]
  // backing this.
  // Should only be used very carefully, since its mutable
  @inline
  def srcBuf: Array[Byte] = srcBuf_

  // How big is the buffer we are reading from
  @inline
  def bufferSiz: Int = readbufferSiz_

  def setBytes(arr: Array[Byte]): Unit = {
    bufferPos = 0
    srcBuf_ = arr
    readbufferSiz_ = srcBuf_.length
  }

  override def isOpen: Boolean = bufferPos < readbufferSiz_

  override def open(): Unit = ()

  override def close(): Unit = ()

  override def readAll(buf: Array[Byte], off: Int, len: Int): Int =
    read(buf, off, len)

  override def read(destBuf: Array[Byte], destOffset: Int, len: Int): Int = {
    System.arraycopy(srcBuf_, bufferPos, destBuf, destOffset, len)
    bufferPos = bufferPos + len
    len
  }

  override def getBufferPosition: Int = bufferPos
  override def getBytesRemainingInBuffer: Int = readbufferSiz_ - bufferPos
  override def getBuffer: Array[Byte] = srcBuf_

  override def consumeBuffer(len: Int): Unit = {
    bufferPos = bufferPos + len
  }
}