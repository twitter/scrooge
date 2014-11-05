package com.twitter.scrooge

import org.apache.thrift.TApplicationException
import org.apache.thrift.TException
import org.apache.thrift.protocol.TMessage
import org.apache.thrift.protocol.TMessageType
import org.apache.thrift.protocol.TProtocol
import org.apache.thrift.protocol.TProtocolException
import org.slf4j.LoggerFactory

abstract class ThriftFunction[I, T <: ThriftStruct](methodName: String) {

  private val Log = LoggerFactory.getLogger(getClass())

  protected val oneWay = false

  protected def decode(in: TProtocol): T

  protected def getResult(iface: I, args: T): ThriftStruct

  def process(seqid: Int, in: TProtocol, out: TProtocol, iface: I): Unit = {
    val args = try{
      decode(in)
    } catch {
      case e: TProtocolException => {
        in.readMessageEnd()
        val x = new TApplicationException(TApplicationException.PROTOCOL_ERROR, e.getMessage())
        out.writeMessageBegin(new TMessage(methodName, TMessageType.EXCEPTION, seqid))
        x.write(out)
        out.writeMessageEnd()
        out.getTransport().flush()
        return
      }
    }
    in.readMessageEnd()

    val result = try {
      getResult(iface, args)
    } catch {
      case e: Throwable => {
        Log.error("Internal error processing " + methodName, e)
        val x = new TApplicationException(TApplicationException.INTERNAL_ERROR, "Internal error processing "+ methodName)
        out.writeMessageBegin(new TMessage(methodName, TMessageType.EXCEPTION, seqid))
        x.write(out)
        out.writeMessageEnd()
        out.getTransport().flush()
        return
      }
    }

    if (!oneWay) {
      out.writeMessageBegin(new TMessage(methodName, TMessageType.REPLY, seqid))
      result.write(out)
      out.writeMessageEnd()
      out.getTransport().flush()
    }
  }

}
