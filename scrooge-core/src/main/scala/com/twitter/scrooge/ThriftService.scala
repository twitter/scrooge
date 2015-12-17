package com.twitter.scrooge


/**
 * A marker trait for interfaces that represent thrift services.
 */
trait ThriftService

/**
 * A trait indicating that this can be converted to a ThriftService
 */
trait ToThriftService {
  /** Create a ThriftService */
  def toThriftService: ThriftService
}