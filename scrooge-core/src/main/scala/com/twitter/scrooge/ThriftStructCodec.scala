package com.twitter.scrooge

import com.twitter.util.Memoize
import org.apache.thrift.protocol.TProtocol
import scala.reflect.ClassTag
import scala.reflect.runtime.universe

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
  private[this] val thriftStructCodecType = universe.typeOf[ThriftStructCodec[_]]

  private[this] val codecForStructClass = Memoize.classValue { c =>
    val runtimeMirror = universe.runtimeMirror(c.getClassLoader)

    val codecSymbol =
      runtimeMirror
        .classSymbol(c)
        .baseClasses.iterator
        .map(_.companion)
        .filter(_.isModule)
        .map(_.asModule)
        .find(_.moduleClass.asType.toType <:< thriftStructCodecType)
        .getOrElse(
          throw new IllegalArgumentException(
            s"No companion ThriftStructCodec found for ${c.getName} or its base classes")
        )

    runtimeMirror.reflectModule(codecSymbol).instance
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

  /**
   * For a given scrooge-generated thrift struct or union class, returns its codec
   *
   * @param classTag the ClassTag or Manifest of the type representing a thrift struct or union
   * @tparam T the thrift struct or union type
   *
   * @return the scrooge-generated codec for the class
   */
  def forStructClassTag[T <: ThriftStruct](classTag: ClassTag[T]): ThriftStructCodec[T] = {
    val clazz = classTag.runtimeClass.asInstanceOf[Class[T]]
    forStructClass(clazz)
  }
}
