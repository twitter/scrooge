package com.twitter.scrooge

object ThriftService {

  /**
   * A string representing reserved method name `asClosable`
   */
  val AsClosableMethodName: String = "asClosable"
}

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
