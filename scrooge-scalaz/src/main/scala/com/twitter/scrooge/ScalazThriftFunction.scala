package com.twitter.scrooge

import org.apache.thrift.protocol.TProtocolException
import org.apache.thrift.protocol.TProtocol
import org.apache.thrift.TApplicationException
import org.apache.thrift.protocol.TMessage
import org.apache.thrift.protocol.TMessageType
import scala.util.control.NonFatal
import scalaz.concurrent.Task
import scalaz._
import scalaz.Scalaz._

abstract class ScalazThriftFunction[I, T <: ThriftStruct](methodName: String) {

  protected val oneWay = false

  protected def decode(in: TProtocol): T

  protected def getResult(iface: I, args: T): Task[ThriftStruct]

  def process(seqid: Int, in: TProtocol, out: TProtocol, iface: I): Unit = {
    val args = try {
      decode(in)
    } catch {
      case e: TProtocolException ⇒ {
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

    getResult(iface, args).runAsync {
      case \/-(result) ⇒
        if (!oneWay) {
          out.writeMessageBegin(new TMessage(methodName, TMessageType.REPLY, seqid))
          result.write(out)
          out.writeMessageEnd()
          out.getTransport().flush()
        }
      case -\/(e) ⇒
        val x = new TApplicationException(TApplicationException.INTERNAL_ERROR, "Internal error processing " + methodName)
        out.writeMessageBegin(new TMessage(methodName, TMessageType.EXCEPTION, seqid))
        x.write(out)
        out.writeMessageEnd()
        out.getTransport().flush()
    }

  }

}