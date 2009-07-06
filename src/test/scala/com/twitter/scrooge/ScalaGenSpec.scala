package com.twitter.scrooge

import java.nio.ByteOrder
import org.apache.mina.core.buffer.IoBuffer
import org.apache.mina.core.filterchain.IoFilter
import org.apache.mina.core.session.{DummySession, IoSession}
import org.apache.mina.filter.codec._
import org.specs._
import net.lag.naggati.{Decoder, End, ProtocolError, Step}
import net.lag.naggati.Steps._
import com.twitter.scrooge.codec._


// generated:
case class Simple(var x: Int, var y: Int) {
  // empty constructor for decoding
  def this() = this(0, 0)

  val F_X = 1
  val F_Y = 2

  def decode(f: Simple => Step): Step = Codec.readStruct(this, f) {
    case (F_X, Type.I32) => Codec.readI32 { v => this.x = v; decode(f) }
    case (F_Y, Type.I32) => Codec.readI32 { v => this.y = v; decode(f) }
    case (_, ftype) => Codec.skip(ftype) { decode(f) }
  }
}


object ScalaGenSpec extends Specification {
  private val fakeSession = new DummySession

  private val fakeDecoderOutput = new ProtocolDecoderOutput {
    override def flush(nextFilter: IoFilter.NextFilter, s: IoSession) = {}
    override def write(obj: AnyRef) = {
      written = obj :: written
    }
  }

  private var written: List[AnyRef] = Nil

  "ScalaGen" should {
    doBefore {
      written = Nil
    }

    "decode a simple struct" in {
      val simple = new Simple()
      val data = IoBuffer.allocate(64)
      data.order(ByteOrder.BIG_ENDIAN)
      data.put(Type.I32.toByte)
      data.putShort(simple.F_X.toShort)
      data.putInt(3)
      data.put(Type.I32.toByte)
      data.putShort(simple.F_Y.toShort)
      data.putInt(4)
      data.put(Type.STOP.toByte)
      data.putShort(0.toShort)
      data.flip()
      new Decoder(simple.decode { x => written = x :: written; End }).decode(fakeSession, data, fakeDecoderOutput)
      written mustEqual List(Simple(3, 4))
    }
  }
}
