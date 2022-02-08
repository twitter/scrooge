package com.twitter.scrooge

import com.twitter.util.Memoize
import org.apache.thrift.protocol.TProtocol

/**
 * A trait encapsulating the logic for encoding and decoding a specific thrift struct
 * type.
 */
trait ThriftStructCodec[T <: ThriftStruct] {
  @throws(classOf[org.apache.thrift.TException])
  def encode(t: T, oprot: TProtocol): Unit

  @throws(classOf[org.apache.thrift.TException])
  def decode(iprot: TProtocol): T

  lazy val metaData: ThriftStructMetaData[T] = ThriftStructMetaData(this)
}

/**
 * This object provides operations to obtain `ThriftStructCodec`
 * instances.
 */
object ThriftStructCodec {
  private[this] val codecForStructClass = Memoize.classValue { c =>
    Class
      .forName(c.getName + "$", true, c.getClassLoader)
      .getField("MODULE$")
      .get(null)
  }

  /**
   * For a given scrooge-generated thrift struct or union class, returns its codec
   *
   * @param c the class representing a thrift struct or union
   * @tparam T the thrift struct or union type
   *
   * @return the scrooge-generated codec for the class
   */
  def forStructClass[T <: ThriftStruct](c: Class[T]): ThriftStructCodec[T] =
    codecForStructClass(c).asInstanceOf[ThriftStructCodec[T]]
}
