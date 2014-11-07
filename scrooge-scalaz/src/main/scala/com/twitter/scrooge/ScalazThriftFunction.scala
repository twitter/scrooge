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
import org.apache.thrift.server.AbstractNonblockingServer
import com.twitter.scrooge.AsyncThriftFunction

/**
 * TODO: common code between this and ThriftFunction could be factored out
 */
abstract class ScalazThriftFunction[I, T <: ThriftStruct](methodName: String) extends AsyncThriftFunction[I] {

  protected val oneWay = false

  protected def decode(in: TProtocol): T

  protected def getResult(iface: I, args: T): Task[ThriftStruct]

  def process(seqid: Int, buffer: AbstractNonblockingServer#AsyncFrameBuffer, iface: I): Unit = {
    val in = buffer.getInputProtocol()
    val out = buffer.getOutputProtocol()
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
        buffer.responseReady()
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
          buffer.responseReady()
        }
      case -\/(e) ⇒
        val x = new TApplicationException(TApplicationException.INTERNAL_ERROR, "Internal error processing " + methodName)
        out.writeMessageBegin(new TMessage(methodName, TMessageType.EXCEPTION, seqid))
        x.write(out)
        out.writeMessageEnd()
        out.getTransport().flush()
        buffer.responseReady()
    }

  }

}
