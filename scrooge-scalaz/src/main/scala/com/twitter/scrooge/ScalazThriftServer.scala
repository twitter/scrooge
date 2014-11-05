package com.twitter.scrooge

import org.apache.thrift.server.AbstractNonblockingServer
import org.apache.thrift.server.AbstractNonblockingServer.AbstractNonblockingServerArgs
import scala.language.existentials

//class ScalazThriftServer(args: AbstractNonblockingServerArgs[T] forSome {type T <: AbstractNonblockingServerArgs[T]}) extends AbstractNonblockingServer(args) {
//  def startThreads(): Boolean = {
//    ???
//  }
//}