package com.twitter.scrooge

import org.apache.thrift.protocol.{TProtocol, TType}

object ThriftStruct {
  def ttypeToString(byte: Byte): String = {
    // from https://github.com/apache/thrift/blob/master/lib/java/src/org/apache/thrift/protocol/TType.java
    byte match {
      case TType.STOP => "STOP"
      case TType.VOID => "VOID"
      case TType.BOOL => "BOOL"
      case TType.BYTE => "BYTE"
      case TType.DOUBLE => "DOUBLE"
      case TType.I16 => "I16"
      case TType.I32 => "I32"
      case TType.I64 => "I64"
      case TType.STRING => "STRING"
      case TType.STRUCT => "STRUCT"
      case TType.MAP => "MAP"
      case TType.SET => "SET"
      case TType.LIST => "LIST"
      case TType.ENUM => "ENUM"
      case _ => "UNKNOWN"
    }
  }
}

trait ThriftStruct extends ThriftStructIface {
  @throws(classOf[org.apache.thrift.TException])
  def write(oprot: TProtocol): Unit
}
