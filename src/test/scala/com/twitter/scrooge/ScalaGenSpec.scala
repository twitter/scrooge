package com.twitter.scrooge

import scala.collection.Map
import scala.collection.mutable
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
  def this() = this(0, 0)

  val F_X = 1
  val F_Y = 2

  def decode(f: Simple => Step): Step = Codec.readStruct(this, f) {
    case (F_X, Type.I32) => Codec.readI32 { v => this.x = v; decode(f) }
    case (F_Y, Type.I32) => Codec.readI32 { v => this.y = v; decode(f) }
    case (_, ftype) => Codec.skip(ftype) { decode(f) }
  }

  def encode(buffer: Buffer) {
    buffer.writeFieldHeader(Type.I32, F_X)
    buffer.writeI32(this.x)
    buffer.writeFieldHeader(Type.I32, F_Y)
    buffer.writeI32(this.y)
  }
}

// generated:
case class Page(var rows: Seq[Int]) {
  def this() = this(List[Int]())

  val F_ROWS = 3

  def decode(f: Page => Step): Step = Codec.readStruct(this, f) {
    case (F_ROWS, Type.LIST) => Codec.readList[Int](Type.I32) { f => Codec.readI32 { item => f(item) } } { v => this.rows = v; decode(f) }
    case (_, ftype) => Codec.skip(ftype) { decode(f) }
  }

  def encode(buffer: Buffer) {
    buffer.writeFieldHeader(Type.LIST, F_ROWS)
    buffer.writeListHeader(Type.I32, this.rows.size); for (item <- this.rows) { buffer.writeI32(item) }
  }
}

// generated:
case class Complex(var stuff: Map[String, Seq[Page]]) {
  def this() = this(mutable.Map.empty[String, Seq[Page]])

  val F_STUFF = 1

  def decode(f: Complex => Step): Step = Codec.readStruct(this, f) {
    case (F_STUFF, Type.MAP) => Codec.readMap[String, Seq[Page]](Type.STRING, Type.LIST) { f => Codec.readString { item => f(item) } } { f => Codec.readList[Page](Type.STRUCT) { f => (new Page).decode { item => f(item) } } { item => f(item) } } { v => this.stuff = v; decode(f) }
    case (_, ftype) => Codec.skip(ftype) { decode(f) }
  }

  def encode(buffer: Buffer) {
    buffer.writeFieldHeader(Type.MAP, F_STUFF)
    buffer.writeMapHeader(Type.STRING, Type.LIST, this.stuff.size); for ((k, v) <- this.stuff) { buffer.writeString(k); buffer.writeListHeader(Type.STRUCT, v.size); for (item <- v) { item.encode(buffer) } }
  }
}


object ScalaGenSpec extends Specification {
  private var fakeSession: IoSession = null

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
      fakeSession = new DummySession
    }

    "decode a simple struct" in {
      val simple = new Simple()
      val data = new Buffer
      data.writeFieldHeader(Type.I32, simple.F_X)
      data.writeI32(3)
      data.writeFieldHeader(Type.I32, simple.F_Y)
      data.writeI32(4)
      data.writeFieldHeader(Type.STOP, 0)
      data.buffer.flip()
      new Decoder(simple.decode { x => written = x :: written; End }).decode(fakeSession, data.buffer, fakeDecoderOutput)
      written mustEqual List(Simple(3, 4))
    }

    "decode a complex struct" in {
      val page = new Page(List(15, 30, 45))
      val complex = new Complex(mutable.Map("first" -> List(page)))
      val complex2 = new Complex
      val data = new Buffer
      data.writeFieldHeader(Type.MAP, complex.F_STUFF)
      data.writeMapHeader(Type.STRING, Type.LIST, 1)
      data.writeString("first")
      data.writeListHeader(Type.STRUCT, 1)
      data.writeFieldHeader(Type.LIST, page.F_ROWS)
      data.writeListHeader(Type.I32, 3)
      data.writeI32(15)
      data.writeI32(30)
      data.writeI32(45)
      data.writeFieldHeader(Type.STOP, 0)
      data.writeFieldHeader(Type.STOP, 0)
      data.buffer.flip()
      new Decoder(complex2.decode { x => written = x :: written; End }).decode(fakeSession, data.buffer, fakeDecoderOutput)
      written mustEqual List(complex)
    }
  }
}
