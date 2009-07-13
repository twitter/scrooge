package com.twitter.scrooge.codec

import org.specs._
import net.lag.extensions._

object CodecSpec extends Specification {
  var buffer: Buffer = null

  def getHex() = {
    buffer.buffer.flip()
    val bytes = new Array[Byte](buffer.buffer.limit)
    buffer.buffer.get(bytes)
    bytes.hexlify()
  }

  "Codec" should {
    doBefore {
      buffer = new Buffer
    }

    "encode" in {
      "boolean" in {
        buffer.writeBoolean(true)
        buffer.writeBoolean(false)
        getHex() mustEqual "0100"
      }

      "byte" in {
        buffer.writeByte(199.toByte)
        getHex() mustEqual "c7"
      }

      "i16" in {
        buffer.writeI16(150)
        getHex() mustEqual "0096"
      }

      "i32" in {
        buffer.writeI32(9876543)
        getHex() mustEqual "0096b43f"
      }

      "i64" in {
        buffer.writeI64(123412351236L)
        getHex() mustEqual "0000001cbbf30904"
      }

      "double" in {
        buffer.writeDouble(9.5)
        getHex() mustEqual "4023000000000000"
      }

      "string" in {
        buffer.writeString("hello")
        getHex() mustEqual "0000000568656c6c6f"
      }

      "binary" in {
        buffer.writeBinary("cat".getBytes)
        getHex() mustEqual "00000003636174"
      }

      "field header" in {
        buffer.writeFieldHeader(12, 44)
        getHex() mustEqual "0c002c"
      }

      "list header" in {
        buffer.writeListHeader(Type.I32, 3)
        getHex() mustEqual "0800000003"
      }

      "map header" in {
        buffer.writeMapHeader(Type.STRING, Type.I32, 1)
        getHex() mustEqual "0b0800000001"
      }

      "set header" in {
        buffer.writeSetHeader(Type.I32, 1)
        getHex() mustEqual "0800000001"
      }
    }
  }
}
