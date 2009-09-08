package com.twitter.scrooge

import scala.collection.Map
import scala.collection.mutable
import org.apache.mina.core.buffer.IoBuffer
import org.apache.mina.core.filterchain.IoFilter
import org.apache.mina.core.session.{DummySession, IoSession}
import org.apache.mina.filter.codec._
import org.specs._
import net.lag.extensions._
import net.lag.naggati.{Decoder, End, ProtocolError, Step}
import net.lag.naggati.Steps._
import com.twitter.scrooge.codec._


// generated:
case class Simple(var x: Int, var y: Int) {
  def this() = this(0, 0)

  val F_X = 1
  val F_Y = 2

  var x__isSet = true
  var y__isSet = true

  def decode(f: Simple => Step): Step = {
    x__isSet = false
    y__isSet = false
    _decode(f)
  }

  def _decode(f: Simple => Step): Step = Codec.readStruct(this, f) {
    case (F_X, Type.I32) =>
      this.x__isSet = true
      Codec.readI32 { v => this.x = v; _decode(f) }

    case (F_Y, Type.I32) =>
      this.y__isSet = true
      Codec.readI32 { v => this.y = v; _decode(f) }

    case (_, ftype) => Codec.skip(ftype) { _decode(f) }
  }

  def encode(buffer: Buffer) {
    if (this.x__isSet) {
      buffer.writeFieldHeader(FieldHeader(Type.I32, F_X))
      buffer.writeI32(this.x)
    }
    if (this.y__isSet) {
      buffer.writeFieldHeader(FieldHeader(Type.I32, F_Y))
      buffer.writeI32(this.y)
    }
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
    buffer.writeFieldHeader(FieldHeader(Type.LIST, F_ROWS))
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
    buffer.writeFieldHeader(FieldHeader(Type.MAP, F_STUFF))
    buffer.writeMapHeader(Type.STRING, Type.LIST, this.stuff.size); for ((k, v) <- this.stuff) { buffer.writeString(k); buffer.writeListHeader(Type.STRUCT, v.size); for (item <- v) { item.encode(buffer) } }
  }
}


object ScalaGenSpec extends Specification {
  private val fakeDecoderOutput = new ProtocolDecoderOutput {
    override def flush(nextFilter: IoFilter.NextFilter, s: IoSession) = {}
    override def write(obj: AnyRef) = {
      written += obj
    }
  }

  private var written = new mutable.ListBuffer[AnyRef]
  private var data: Buffer = null

  val simpleEncoding = "080001000000190800020000001a"
  val partialSimpleEncoding = "08000200000006"
  val complexEncoding = "0d00010b0f00000002000000037265640c000000020f000308000000030000000f00000010000000110f00030800000002000000150000001600000004626c75650c00000000"

  def getHex() = {
    data.buffer.flip()
    val bytes = new Array[Byte](data.buffer.limit)
    data.buffer.get(bytes)
    bytes.hexlify()
  }

  def decode(buffer: Buffer, step: Step) = {
    new Decoder(step).decode(new DummySession, buffer.buffer, fakeDecoderOutput)
  }


  "ScalaGen" should {
    doBefore {
      written.clear()
      data = new Buffer
    }

    "encode" in {
      "a simple struct" in {
        val simple = new Simple(25, 26)
        simple.encode(data)
        getHex() mustEqual simpleEncoding
      }

      "a partial simple struct" in {
        val simple = new Simple(9, 6)
        simple.x__isSet = false
        simple.encode(data)
        getHex() mustEqual partialSimpleEncoding
      }

      "a complex struct" in {
        val page1 = new Page(List(15, 16, 17))
        val page2 = new Page(List(21, 22))
        val complex = new Complex(mutable.Map("red" -> List(page1, page2), "blue" -> Nil))
        complex.encode(data)
        getHex() mustEqual complexEncoding
      }
    }

    "decode" in {
      "a simple struct" in {
        val simple = new Simple()
        data.writeFieldHeader(FieldHeader(Type.I32, simple.F_X))
        data.writeI32(3)
        data.writeFieldHeader(FieldHeader(Type.I32, simple.F_Y))
        data.writeI32(4)
        data.writeFieldHeader(FieldHeader(Type.STOP, 0))
        data.buffer.flip()
        decode(data, simple.decode { x => written += x; End })
        written.toList mustEqual List(Simple(3, 4))
      }

      "a partial simple struct" in {
        val simple = new Simple()
        data.writeFieldHeader(FieldHeader(Type.I32, simple.F_X))
        data.writeI32(3)
        data.writeFieldHeader(FieldHeader(Type.STOP, 0))
        data.buffer.flip()
        decode(data, simple.decode { x => written += x; End })
        written.toList mustEqual List(Simple(3, 0))
        val writtenSimple = written(0).asInstanceOf[Simple]
        writtenSimple.x__isSet mustBe true
        writtenSimple.y__isSet mustBe false
      }

      "a complex struct" in {
        val page = new Page(List(15, 30, 45))
        val complex = new Complex(mutable.Map("first" -> List(page)))
        val complex2 = new Complex
        data.writeFieldHeader(FieldHeader(Type.MAP, complex.F_STUFF))
        data.writeMapHeader(Type.STRING, Type.LIST, 1)
        data.writeString("first")
        data.writeListHeader(Type.STRUCT, 1)
        data.writeFieldHeader(FieldHeader(Type.LIST, page.F_ROWS))
        data.writeListHeader(Type.I32, 3)
        data.writeI32(15)
        data.writeI32(30)
        data.writeI32(45)
        data.writeFieldHeader(FieldHeader(Type.STOP, 0))
        data.writeFieldHeader(FieldHeader(Type.STOP, 0))
        data.buffer.flip()
        decode(data, complex2.decode { x => written += x; End })
        written.toList mustEqual List(complex)
      }
    }

    "process" in {
      val impl = new generated.Example1() {
        def get_id(name: String): Int = name.length()
      }
      val processor = generated.Example1.processor(impl)
      val out = new Buffer()
      out.writeRequestHeader(RequestHeader(MessageType.CALL, "get_id", 23))
      (new generated.Example1.get_id_args("cat")).encode(out)
      out.buffer.flip()
      decode(out, processor())

      data = written(0).asInstanceOf[Buffer]
      written.clear
      data.buffer.flip()
      decode(data, Codec.readRequestHeader { request => written += request; (new generated.Example1.get_id_result()).decode { x => written += x._rv.toString; End } })
      written.toList mustEqual List(RequestHeader(MessageType.REPLY, "get_id", 23), "3")
    }
  }
}
