package com.twitter.scrooge

import org.apache.thrift.TProcessor
import org.apache.thrift.protocol.TProtocol

class ScalazThriftProcessor extends TProcessor {
  override def process(in: TProtocol, out: TProtocol): Boolean = {
    val msg = in.readMessageBegin()
    val function = processMap.getOrElse(msg.name, null)
    if (function != null) {
      function.process(msg.seqid, in, out, iface)
    } else {
      TProtocolUtil.skip(in, TType.STRUCT)
      in.readMessageEnd()
      val x = new TApplicationException(TApplicationException.UNKNOWN_METHOD, "Invalid method name: '"+msg.name+"'")
      out.writeMessageBegin(new TMessage(msg.name, TMessageType.EXCEPTION, msg.seqid))
      x.write(out)
      out.writeMessageEnd()
      out.getTransport().flush()
    }
    true
  }
}