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
    def getCodec(className: String) =
      Class
        .forName(className, true, c.getClassLoader)
        .getField("MODULE$")
        .get(null)
        .asInstanceOf[ThriftStructCodec[_]]

    val cname = c.getName
    try {
      // Most struct classes will have their
      // companion object as the decoder.
      getCodec(cname + "$")
    } catch {
      case e1: Exception =>
        val i = cname.lastIndexOf('$')
        if (i == -1) {
          throw e1;
        }
        try {
          // Some struct classes (LazyImmutable,
          // union members) will have the object
          // they're embedded in as their decoder.
          getCodec(cname.substring(0, i + 1))
        } catch {
          case e2: Exception =>
            e2.addSuppressed(e1)
            throw e2
        }
    }
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
