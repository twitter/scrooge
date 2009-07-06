package com.twitter.scrooge.codec

import java.io.IOException
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

/*
  def readList[T](itemtype: Int)(f: => Step) = {
    readInt8 { itype =>
      if (itype == itemtype) {
        
      } else {
        skip(itemtype) { f(null) }
      }
    }
    def read_list(s)
      etype, len = s.read_all(5).unpack("cN")
      rv = []
      len.times do
        rv << read_value(s, etype)
      end
      rv
    end
  }
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

  /*
  def readRequestArgs(messageName: String, args: List[])

  def read_value(s, type)
    case type
    when Types::I64
      hi, lo = s.read_all(8).unpack("NN")
      (hi << 32) | lo
    when Types::STRUCT
      read_struct(s)
    when Types::MAP
      read_map(s)
    else
      s.read_all(SIZES[type]).unpack(FORMATS[type]).first
    end
  end

  def readField(process: Option[] => Step) = {
    readInt8 { c =>
      if (c == Types.STOP) {
        None
      } else {
        readInt16BE { fid => readValue()
          
        }
      }
    <
    vdef read_field(s)
      type = s.read_all(1).unpack("c").first
      return nil if type == Types::STOP
      fid = s.read_all(2).unpack("n").first
      read_value(s, type)
    end
    
    */
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




/*


  val decoder = new Decoder(readLine(true, "ISO-8859-1") { line =>
    val segments = line.split(" ")
    segments(0) = segments(0).toUpperCase

    val command = segments(0)
    if (! KNOWN_COMMANDS.contains(command)) {
      throw new ProtocolError("Invalid command: " + command)
    }

    if (DATA_COMMANDS.contains(command)) {
      if (segments.length < 5) {
        throw new ProtocolError("Malformed request line")
      }
      val dataBytes = segments(4).toInt
      readBytes(dataBytes + 2) {
        // final 2 bytes are just "\r\n" mandated by protocol.
        val bytes = new Array[Byte](dataBytes)
        state.buffer.get(bytes)
        state.buffer.position(state.buffer.position + 2)
        state.out.write(Request(segments.toList, Some(bytes)))
        End
      }
    } else {
      state.out.write(Request(segments.toList, None))
      End
    }
  })
  
*/
