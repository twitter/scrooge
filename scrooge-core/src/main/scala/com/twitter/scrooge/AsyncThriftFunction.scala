package com.twitter.scrooge

import org.apache.thrift.protocol.TProtocolException
import org.apache.thrift.protocol.TProtocol
import org.slf4j.LoggerFactory
import org.apache.thrift.TApplicationException
import org.apache.thrift.protocol.TMessage
import org.apache.thrift.protocol.TMessageType
import org.apache.thrift.server.AbstractNonblockingServer

trait AsyncThriftFunction[I] {

  def process(seqid: Int, buf: AbstractNonblockingServer#AsyncFrameBuffer, iface: I): Unit

}
