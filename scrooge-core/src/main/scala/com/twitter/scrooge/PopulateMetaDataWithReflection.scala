package com.twitter.scrooge

import com.twitter.scrooge.internal.ThriftStructMetaDataUtil
import org.apache.thrift.protocol.TField

/**
 * An object containing static methods that retrieves metadata through reflection.
 */
private object PopulateMetaDataWithReflection {

  /**
   * Using reflection, get a Seq of [[ThriftStructField]]s representing
   * the fields defined in the ThriftStruct. This will return an
   * empty Seq for unions.
   */
  def getFieldsWithReflection[T <: ThriftStruct](
    codec: ThriftStructCodec[T],
    metaDataUtil: ThriftStructMetaDataUtil[T]
  ): Seq[ThriftStructField[T]] =
    if (metaDataUtil.isUnion()) {
      Nil
    } else {
      metaDataUtil.structCodecClass.getMethods.toList
        .filter { m => m.getParameterTypes.length == 0 && m.getReturnType == classOf[TField] }
        .map { m =>
          val tfield = m.invoke(codec).asInstanceOf[TField]
          val manifest: scala.Option[Manifest[_]] =
            try {
              Some {
                metaDataUtil.structCodecClass
                  .getMethod(m.getName + "Manifest")
                  .invoke(codec)
                  .asInstanceOf[Manifest[_]]
              }
            } catch { case _: Throwable => None }
          new ThriftStructField[T](tfield, manifest, metaDataUtil.thriftStructClass) {
            def getValue[R](struct: T): R = method.invoke(struct).asInstanceOf[R]
          }
        }
    }

  /**
   * Using reflection, get a Seq of [[ThriftStructFieldInfo]]s
   * representing the fields defined in the ThriftStruct.
   * This will return an empty Seq for unions.
   */
  def getFieldInfosWithReflection[T <: ThriftStruct](
    codec: ThriftStructCodec[T],
    metaDataUtil: ThriftStructMetaDataUtil[T]
  ): Seq[ThriftStructFieldInfo] =
    if (metaDataUtil.isUnion()) {
      Nil
    } else {
      metaDataUtil.structCodecClass
        .getMethod("fieldInfos")
        .invoke(codec)
        .asInstanceOf[Seq[ThriftStructFieldInfo]]
    }

  /**
   * Using reflection, get a Seq of [[ThriftUnionFieldInfo]]s representing the
   * fields of the union. This will return an empty Seq for non-unions.
   */
  def getUnionFieldsWithReflection[T <: ThriftStruct](
    codec: ThriftStructCodec[T],
    metaDataUtil: ThriftStructMetaDataUtil[T]
  ): Seq[ThriftUnionFieldInfo[ThriftUnion with ThriftStruct, _]] =
    if (!metaDataUtil.isUnion()) {
      Nil
    } else {
      metaDataUtil.structCodecClass
        .getMethod("fieldInfos")
        .invoke(codec)
        .asInstanceOf[Seq[ThriftUnionFieldInfo[ThriftUnion with ThriftStruct, _]]]
    }

  /**
   * Using reflection, get the parsed annotations at the struct or union level.
   * Left hand side of equal sign is the key, right side is the value.
   */
  def getStructAnnotationsWithReflection[T <: ThriftStruct](
    codec: ThriftStructCodec[T],
    metaDataUtil: ThriftStructMetaDataUtil[T]
  ): Map[String, String] =
    metaDataUtil.structCodecClass
      .getMethod("structAnnotations")
      .invoke(codec)
      .asInstanceOf[Map[String, String]]
}
