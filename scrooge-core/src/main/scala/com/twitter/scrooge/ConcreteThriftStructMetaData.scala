package com.twitter.scrooge

import com.twitter.scrooge.internal.ThriftStructMetaDataUtil

/**
 * An implmentation of [[ThriftStructMetaData]] where much of the information
 * is provided via its constructor.
 */
final private[scrooge] class ConcreteThriftStructMetaData[T <: ThriftStruct](
  val codec: ThriftStructCodec[T],
  val fields: Seq[ThriftStructField[T]],
  val fieldInfos: Seq[ThriftStructFieldInfo],
  val unionFields: Seq[ThriftUnionFieldInfo[ThriftUnion, _]],
  val structAnnotations: Map[String, String])
    extends ThriftStructMetaData[T] {

  // The implementation details of this set of values are duplicated between
  // here and `ReflectionThriftStructMetaData`. This is done on purpose to
  // not pollute the abstract `ThriftStructMetaData` class while additional
  // changes are ongoing.
  private[this] val metaDataUtil = new ThriftStructMetaDataUtil[T](codec)
  def codecClass: Class[_] = metaDataUtil.structCodecClass
  def structClassName: String = metaDataUtil.thriftStructSubClassName
  def structName: String = metaDataUtil.thriftStructName
  def structClass: Class[T] = metaDataUtil.thriftStructClass
}
