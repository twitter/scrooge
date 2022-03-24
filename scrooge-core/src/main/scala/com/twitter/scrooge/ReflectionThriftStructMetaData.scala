package com.twitter.scrooge

import com.twitter.scrooge.internal.ThriftStructMetaDataUtil

/**
 * An implementation of [[ThriftStructMetaData]] where much of the information
 * is provided via reflection.
 */
final private[scrooge] class ReflectionThriftStructMetaData[T <: ThriftStruct](
  val codec: ThriftStructCodec[T])
    extends ThriftStructMetaData[T] {

  private[this] val metaDataUtil = new ThriftStructMetaDataUtil[T](codec)
  def codecClass: Class[_] = metaDataUtil.structCodecClass
  def structClassName: String = metaDataUtil.thriftStructSubClassName
  def structName: String = metaDataUtil.thriftStructName
  def structClass: Class[T] = metaDataUtil.thriftStructClass

  val fields: Seq[ThriftStructField[T]] =
    PopulateMetaDataWithReflection.getFieldsWithReflection[T](codec, metaDataUtil)
  val fieldInfos: Seq[ThriftStructFieldInfo] =
    PopulateMetaDataWithReflection.getFieldInfosWithReflection[T](codec, metaDataUtil)
  val unionFields: Seq[ThriftUnionFieldInfo[ThriftUnion, _]] =
    PopulateMetaDataWithReflection.getUnionFieldsWithReflection[T](codec, metaDataUtil)
  val structAnnotations: Map[String, String] =
    PopulateMetaDataWithReflection.getStructAnnotationsWithReflection[T](codec, metaDataUtil)
}
