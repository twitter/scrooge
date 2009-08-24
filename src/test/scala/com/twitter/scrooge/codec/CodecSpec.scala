package com.twitter.scrooge.codec

import java.nio.ByteOrder
import scala.collection.mutable
import org.apache.mina.core.buffer.IoBuffer
import org.apache.mina.core.filterchain.IoFilter
import org.apache.mina.core.session.{DummySession, IoSession}
import org.apache.mina.filter.codec._
import org.specs._
import net.lag.extensions._
import net.lag.naggati.{Decoder, End, ProtocolError, Step, TestDecoder}

object CodecSpec extends Specification {
  var buffer: Buffer = null
  var fakeSession: IoSession = null
  var written: mutable.ListBuffer[AnyRef] = null
  var decoder: TestDecoder = null

  private val fakeDecoderOutput = new ProtocolDecoderOutput {
    override def flush(nextFilter: IoFilter.NextFilter, s: IoSession) = {}
    override def write(obj: AnyRef) = written += obj
  }

  def getHex() = {
    buffer.buffer.flip()
    val bytes = new Array[Byte](buffer.buffer.limit)
    buffer.buffer.get(bytes)
    bytes.hexlify()
  }

  def makeBuffer(s: String) = {
    val buffer = IoBuffer.wrap(s.unhexlify)
    buffer.order(ByteOrder.BIG_ENDIAN)
    buffer
  }


  "Codec" should {
    doBefore {
      buffer = new Buffer
      fakeSession = new DummySession
      written = new mutable.ListBuffer[AnyRef]
      decoder = new TestDecoder
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

      "field header" in {
        buffer.writeFieldHeader(FieldHeader(12, 44))
        getHex() mustEqual "0c002c"
      }

      "request header" in {
        buffer.writeRequestHeader(RequestHeader(MessageType.CALL, "getHeight", 23))
        getHex() mustEqual "800100010000000967657448656967687400000017"
      }
    }

    "decode" in {
      "boolean" in {
        decoder(makeBuffer("0100"), Codec.readBoolean { x => decoder.write(x.toString); End }) mustEqual List("true", "false")
      }

      "byte" in {
        decoder(makeBuffer("c7"), Codec.readByte { x => decoder.write(x.toString); End }) mustEqual List("-57")
      }

      "i16" in {
        decoder(makeBuffer("0096"), Codec.readI16 { x => decoder.write(x.toString); End }) mustEqual List("150")
      }

      "i32" in {
        decoder(makeBuffer("0096b43f"), Codec.readI32 { x => decoder.write(x.toString); End }) mustEqual List("9876543")
      }

      "i64" in {
        decoder(makeBuffer("0000001cbbf30904"), Codec.readI64 { x => decoder.write(x.toString); End }) mustEqual List("123412351236")
      }

      "double" in {
        decoder(makeBuffer("4023000000000000"), Codec.readDouble { x => decoder.write(x.toString); End }) mustEqual List("9.5")
      }

      "string" in {
        decoder(makeBuffer("0000000568656c6c6f"), Codec.readString { x => decoder.write(x.toString); End }) mustEqual List("hello")
      }

      "binary" in {
        decoder(makeBuffer("00000003636174"), Codec.readBinary { x => decoder.write(new String(x)); End }) mustEqual List("cat")
      }

      "list" in {
        decoder(makeBuffer("08000000030096b43f0096b43f0096b43f"), Codec.readList[Int](Type.I32) { f => Codec.readI32 { item => f(item) } } { x => decoder.write(x.toString); End }) mustEqual List("List(9876543, 9876543, 9876543)")
      }

      "map" in {
        decoder(makeBuffer("0b0800000001000000036361740096b43f"), Codec.readMap[String, Int](Type.STRING, Type.I32) { f => Codec.readString { item => f(item) } } { f => Codec.readI32 { item => f(item) } } { x => decoder.write(x.toString); End }) mustEqual List("Map(cat -> 9876543)")
      }

      "set" in {
        decoder(makeBuffer("0800000001000000ff"), Codec.readSet[Int](Type.I32) { f => Codec.readI32 { item => f(item) } } { x => decoder.write(x.toString); End }) mustEqual List("Set(255)")
      }

      "field header" in {
        decoder(makeBuffer("0c002c"), Codec.readFieldHeader { x => decoder.write(x.toString); End }) mustEqual List("FieldHeader(12,44)")
      }

      "request header" in {
        decoder(makeBuffer("800100010000000967657448656967687400000017"), Codec.readRequestHeader { x => decoder.write(x.toString); End }) mustEqual List("RequestHeader(1,getHeight,23)")
      }
    }

    "skip" in {
      decoder = new TestDecoder
      decoder(makeBuffer("0123"), Codec.skip(Type.BOOL) { Codec.readByte { x => decoder.write(x.toString); End } }) mustEqual List("35")
      decoder = new TestDecoder
      decoder(makeBuffer("c723"), Codec.skip(Type.BYTE) { Codec.readByte { x => decoder.write(x.toString); End } }) mustEqual List("35")
      decoder = new TestDecoder
      decoder(makeBuffer("009623"), Codec.skip(Type.I16) { Codec.readByte { x => decoder.write(x.toString); End } }) mustEqual List("35")
      decoder = new TestDecoder
      decoder(makeBuffer("0096b43f23"), Codec.skip(Type.I32) { Codec.readByte { x => decoder.write(x.toString); End } }) mustEqual List("35")
      decoder = new TestDecoder
      decoder(makeBuffer("0000001cbbf3090423"), Codec.skip(Type.I64) { Codec.readByte { x => decoder.write(x.toString); End } }) mustEqual List("35")
      decoder = new TestDecoder
      decoder(makeBuffer("402300000000000023"), Codec.skip(Type.DOUBLE) { Codec.readByte { x => decoder.write(x.toString); End } }) mustEqual List("35")
      decoder = new TestDecoder
      decoder(makeBuffer("0000000568656c6c6f23"), Codec.skip(Type.STRING) { Codec.readByte { x => decoder.write(x.toString); End } }) mustEqual List("35")
      decoder = new TestDecoder
      decoder(makeBuffer("0301ff1f00000023"), Codec.skip(Type.STRUCT) { Codec.readByte { x => decoder.write(x.toString); End } }) mustEqual List("35")
      decoder = new TestDecoder
      decoder(makeBuffer("08000000010096b43f23"), Codec.skip(Type.LIST) { Codec.readByte { x => decoder.write(x.toString); End } }) mustEqual List("35")
      decoder = new TestDecoder
      decoder(makeBuffer("0b0800000001000000036361740096b43f23"), Codec.skip(Type.MAP) { Codec.readByte { x => decoder.write(x.toString); End } }) mustEqual List("35")
      decoder = new TestDecoder
      decoder(makeBuffer("0800000001000000ff23"), Codec.skip(Type.SET) { Codec.readByte { x => decoder.write(x.toString); End } }) mustEqual List("35")
    }
  }
}
