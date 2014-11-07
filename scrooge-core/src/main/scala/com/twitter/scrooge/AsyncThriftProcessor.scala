package com.twitter.scrooge

import org.apache.thrift.TApplicationException
import org.apache.thrift.TProcessor
import org.apache.thrift.protocol.TMessage
import org.apache.thrift.protocol.TMessageType
import org.apache.thrift.protocol.TProtocol
import org.apache.thrift.protocol.TProtocolUtil
import org.apache.thrift.protocol.TType
import org.apache.thrift.TAsyncProcessor
import org.apache.thrift.server.AbstractNonblockingServer

abstract class AsyncThriftProcessor[I](iface: I) extends TAsyncProcessor {

  protected val processMap: Map[String, AsyncThriftFunction[I]]

  override def process(fb: AbstractNonblockingServer#AsyncFrameBuffer): Boolean = {
    val in = fb.getInputProtocol()
    val out = fb.getOutputProtocol()
    val msg = in.readMessageBegin()
    val function = processMap.getOrElse(msg.name, null)
    if (function != null) {
      function.process(msg.seqid, fb, iface)
    } else {
      TProtocolUtil.skip(in, TType.STRUCT)
      in.readMessageEnd()
      val x = new TApplicationException(TApplicationException.UNKNOWN_METHOD, "Invalid method name: '"+msg.name+"'")
      out.writeMessageBegin(new TMessage(msg.name, TMessageType.EXCEPTION, msg.seqid))
      x.write(out)
      out.writeMessageEnd()
      out.getTransport().flush()
      fb.responseReady()
    }
    true
  }

}
