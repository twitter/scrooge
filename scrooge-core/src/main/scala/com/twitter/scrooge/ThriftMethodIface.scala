package com.twitter.scrooge

/**
 * Abstract `ThriftMethod` interface for Java thrift methods.
 */
abstract class ThriftMethodIface {

  /** Thrift method name */
  def name: String

  /** Thrift service name. A thrift service is a list of methods. */
  def serviceName: String
}
