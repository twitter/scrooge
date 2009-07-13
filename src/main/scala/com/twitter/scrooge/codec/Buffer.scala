package com.twitter.scrooge.codec

import java.nio.ByteOrder
import scala.reflect.Manifest
import org.apache.mina.core.buffer.IoBuffer

class Buffer {
  val buffer = IoBuffer.allocate(1024, false)
  buffer.setAutoExpand(true)
  buffer.order(ByteOrder.BIG_ENDIAN)

  def writeBoolean(n: Boolean) = buffer.put(if (n) 1.toByte else 0.toByte)
  def writeByte(n: Byte) = buffer.put(n)
  def writeI16(n: Short) = buffer.putShort(n)
  def writeI32(n: Int) = buffer.putInt(n)
  def writeI64(n: Long) = buffer.putLong(n)
  def writeDouble(n: Double) = buffer.putDouble(n)
  def writeString(s: String) = {
    writeI32(s.length)
    buffer.put(s.getBytes)
  }
  def writeBinary(x: Array[Byte]) = {
    writeI32(x.size)
    buffer.put(x)
  }

  def writeFieldHeader(ftype: Int, fid: Int) = {
    buffer.put(ftype.toByte)
    buffer.putShort(fid.toShort)
  }

  def writeListHeader(itemtype: Int, size: Int) = {
    buffer.put(itemtype.toByte)
    buffer.putInt(size)
  }

  def writeMapHeader(keytype: Int, valuetype: Int, size: Int) = {
    buffer.put(keytype.toByte)
    buffer.put(valuetype.toByte)
    buffer.putInt(size)
  }

  def writeSetHeader(itemtype: Int, size: Int) = {
    buffer.put(itemtype.toByte)
    buffer.putInt(size)
  }

 /* def typeFor[T](cls: Class[T]) = {
     if (classOf[Boolean] isAssignableFrom cls) {
       Type.BOOL
     } else if (classOf[Byte] isAssignableFrom cls) {
       Type.BYTE
     } else if (classOf[Double] isAssignableFrom cls) {
       Type.DOUBLE
     } else if (classOf[Short] isAssignableFrom cls) {
       Type.I16
     } else if (classOf[Int] isAssignableFrom cls) {
       Type.I32
     } else {
       Type.STRUCT
     }
    case Boolean => 
     case Byte => Type.BYTE
     case Double => 
     case Short => 
     case Int => 
     case Long => Type.I64
     case String => Type.STRING
     case Seq => Type.LIST
     case Map => Type.MAP
     case Set => Type.SET
     case _ => Type.STRUCT
 * }

   def writeValue[T](item: T)(implicit manifest: Manifest[T]) {
     item match {
       case x: Boolean => writeBoolean(x)
       case x: Byte => writeByte(x)
       case x: Double => writeDouble(x)
       case x: Short => writeI16(x)
       case x: Int => writeI32(x)
       case x: Long => writeI64(x)
       case x: String => writeString(x)

       // FIXME
       case x: Seq[_] => writeList(x)
       val STRUCT = 12
       val MAP = 13
       val SET = 14
       val LIST = 15
 *      
     
     }
   }
   def writeList[T](list: Seq[T])(implicit manifest: Manifest[T]) = {
     buffer.put(typeFor(manifest.erasure).toByte)
     buffer.putInt(list.size)
     for (item <- list) writeValue(item)
   }
   */

}


