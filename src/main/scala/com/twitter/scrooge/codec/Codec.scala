package com.twitter.scrooge.codec

import java.io.IOException
import scala.collection.mutable
import net.lag.naggati.{Decoder, End, ProtocolError, Step}
import net.lag.naggati.Steps._

object Type {
  val STOP = 0
  val VOID = 1
  val BOOL = 2
  val BYTE = 3
  val DOUBLE = 4
  val I16 = 6
  val I32 = 8
  val I64 = 10
  val STRING = 11
  val STRUCT = 12
  val MAP = 13
  val SET = 14
  val LIST = 15
}

object MessageType {
  val CALL = 1
  val REPLY = 2
  val EXCEPTION = 3
}

case class ProtocolException(reason: String) extends IOException(reason)

trait Decodable[T] {
  def decode(f: T => Step): Step
}

/*
def pack_call(method_name, *args)
  [ VERSION_1, MessageTypes::CALL, method_name.size, method_name, 0, pack_struct(args) ].pack("nnNa*Na*")
end
def pack_struct(fields)
  fields.map { |field| pack_field(*field) }.join + [ Types::STOP ].pack("c")
end
def pack_field(name, type, fid, value)
  [ type, fid, pack_value(type, value) ].pack("cna*")
end
s.write_all(pack_call("stats", [ "reset", Types::BOOL, -1, false ]))

*/


case class FieldHeader(ftype: Int, fid: Int)

object Codec {
  val VERSION_1 = 0x8001

  def readBoolean(f: Boolean => Step) = readInt8 { b => f(b != 0) }
  def readByte(f: Byte => Step) = readInt8 { b => f(b) }
  def readI16(f: Short => Step) = readInt16(f)
  def readI32(f: Int => Step) = readInt32(f)
  def readI64(f: Long => Step) = readInt64(f)
  def readDouble(f: Double => Step) = readDoubleBE(f)
  def readString(f: String => Step) = readI32 { len => readByteBuffer(len) { buffer => f(new String(buffer)) } }
  def readBinary(f: Array[Byte] => Step) = readI32 { len => readByteBuffer(len)(f) }

  def readList[T](itemtype: Int)(itemDecoder: (T => Step) => Step)(f: Seq[T] => Step) = {
    val list = new mutable.ListBuffer[T]
    readInt8 { itype =>
      readInt32 { len =>
        if (itype == itemtype) {
//          readN(len, itype, itemDecoder) { }
          readListItems(list, len, itemDecoder, f)
        } else {
          skipN(len, itype) { f(list) }
        }
      }
    }
  }

  def readListItems[T](list: mutable.ListBuffer[T], len: Int, itemDecoder: (T => Step) => Step, f: Seq[T] => Step): Step = {
    if (len == 0) {
      f(list)
    } else {
      itemDecoder { item =>
        list += item
        readListItems(list, len - 1, itemDecoder, f)
      }
    }
  }

  private def readN[T](count: Int, ftype: Int, itemDecoder: (T => Step) => Step)(itemProcessor: T => Unit)(f: => Step): Step = {
    if (count == 0) {
      f
    } else {
      itemDecoder { item =>
        itemProcessor(item)
        readN(count - 1, ftype, itemDecoder)(itemProcessor)(f)
      }
    }
  }


//  def readNItems[T]()
  
  /*
  def readMap[K, V](f: Map[K, V] => Step): Step = readInt8 { ktype => readInt8 { vtype => readInt32 { len =>
    val map = new mutable.HashMap[K, V]
    readMap(ktype, vtype, len, new mutable.HashMap[K, V], f)
  }}}

  def readMap[K, V](ktype: Int, vtype: Int, len: Int, map: mutable.HashMap[K, V], f: Map[K, V] => Step): Step = {
    if (len > 0) {
      readValue(ktype) { k => readValue(vtype) { v => map(k) = v; readMap(ktype, vtype, len - 1, map, f) }}
    } else {
      f(map)
    }
  }
  */


  def readStruct[T](struct: T, f: T => Step)(decoder: PartialFunction[(Int, Int), Step]) = readFieldHeader { fieldHeader =>
    if (fieldHeader.ftype == Type.STOP) {
      f(struct)
    } else {
      decoder(fieldHeader.fid, fieldHeader.ftype)
    }
  }

  def readFieldHeader(f: FieldHeader => Step) = readInt8 { ftype => readInt16BE { fid => f(FieldHeader(ftype, fid)) } }

  def skip(ftype: Int)(f: => Step): Step = ftype match {
    case Type.STOP => f
    case Type.VOID => f
    case Type.BOOL => readBoolean { x => f }
    case Type.BYTE => readByte { x => f }
    case Type.DOUBLE => readDouble { x => f }
    case Type.I16 => readI16 { x => f }
    case Type.I32 => readI32 { x => f }
    case Type.I64 => readI64 { x => f }
    case Type.STRING => readString { x => f }
    case Type.STRUCT => skipStruct(f)
/*    case Type.MAP =>
    case Type.SET =>
    case Type.LIST =>
    */
  }

  private def skipStruct(f: => Step): Step = {
    readStruct[Unit](null, { _ => f }) { case (_, ftype) => Codec.skip(ftype) { skipStruct(f) } }
  }

  private def skipN(count: Int, ftype: Int)(f: => Step): Step = {
    if (count == 0) {
      f
    } else {
      skip(ftype) { skipN(count - 1, ftype)(f) }
    }
  }


  def readRequest() = {
    readInt64BE { header =>
      val version = (header >> 48) & 0xffff
      if (version != VERSION_1) throw new ProtocolException("Illegal protocol version")
      val messageType = (header >> 32) & 0xffff
      if (messageType != MessageType.CALL) throw new ProtocolException("Expected CALL, got " + messageType)
      val messageNameSize = (header & 0xffffffff)
      readByteBuffer(messageNameSize.toInt) { buffer =>
        val messageName = new String(buffer)
        readInt32BE { sequenceId => End
//          readRequestArgs(messageName, Nil)
        }
      }
    }
  }
}



case class Membership(var destination_id: Long, var position: Long, var updated_at: Int, var count: Int) {
  // empty constructor for decoding
  def this() = this(0L, 0L, 0, 0)

  val F_DESTINATION_ID = 1
  val F_POSITION = 2
  val F_UPDATED_AT = 3
  val F_COUNT = 4

  def decode(f: Membership => Step): Step = Codec.readStruct(this, f) {
    case (F_DESTINATION_ID, Type.I64) => Codec.readI64 { v => this.destination_id = v; decode(f) }
    case (F_POSITION, Type.I64) => Codec.readI64 { v => this.position = v; decode(f) }
    case (F_UPDATED_AT, Type.I32) => Codec.readI32 { v => this.updated_at = v; decode(f) }
    case (F_COUNT, Type.I32) => Codec.readI32 { v => this.count = v; decode(f) }
    case (_, ftype) => Codec.skip(ftype) { decode(f) }
  }
}


case class Page(var count: Int, var cursor: Long, var results: Seq[Int], var data: Array[Byte]) {
  // empty constructor for decoding
  def this() = this(0, 0L, List[Int](), null)

  val F_COUNT = 1
  val F_CURSOR = 2
  val F_RESULTS = 3
  val F_OK = 0

  def decode(f: Page => Step): Step = Codec.readStruct(this, f) {
    case (F_COUNT, Type.I32) => Codec.readI32 { v => this.count = v; decode(f) }
    case (F_CURSOR, Type.I64) => Codec.readI64 { v => this.cursor = v; decode(f) }
    case (F_RESULTS, Type.LIST) => Codec.readList[Int](Type.I32) { f => Codec.readI32 { item => f(item) } } { v => this.results = v; decode(f) }
    case (F_OK, Type.STRING) => Codec.readBinary { v => this.data = v; decode(f) }
    case (_, ftype) => Codec.skip(ftype) { decode(f) }
  }
}
