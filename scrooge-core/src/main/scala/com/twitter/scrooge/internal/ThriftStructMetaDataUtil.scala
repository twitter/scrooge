package com.twitter.scrooge.internal

import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec, ThriftUnion}

/**
 * A class for introspecting a [[ThriftStruct]] to get its name and class
 * information. It uses reflection to get the required information from the
 * codec, so use should be avoided as much as possible.
 *
 * @note This class is public but within the `com.twitter.scrooge.internal`
 * namespace, and thus should only be used within scrooge or from generated
 * code.
 */
final class ThriftStructMetaDataUtil[T <: ThriftStruct](
  codec: ThriftStructCodec[T]) {

  /**
   * The Class object for the ThriftStructCodec subclass.
   */
  def structCodecClass: Class[_] = codec.getClass

  /**
   * The fully qualified name of the ThriftStruct subclass.
   */
  val thriftStructSubClassName: String =
    structCodecClass.getName.dropRight(1) // drop '$' from object name

  /**
   * Gets the unqualified name of the struct.
   */
  val thriftStructName: String = thriftStructSubClassName.split("\\.").last

  /**
   * The Class object for ThriftStruct subclass.
   *
   * For a union, this is the parent trait of all branches for the union.
   */
  val thriftStructClass: Class[T] =
    structCodecClass.getClassLoader.loadClass(thriftStructSubClassName).asInstanceOf[Class[T]]

  def isUnion(): Boolean = classOf[ThriftUnion].isAssignableFrom(thriftStructClass)
}
