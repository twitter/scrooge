package com.twitter.scrooge

/**
 * A trait for generic introspection on ThriftStruct classes.
 */
trait ThriftStructMetaData[T <: ThriftStruct] {

  /**
   * The '(en)coder-decoder', hence 'codec', which knows
   * how to convert the ThriftStruct from and to its
   * wire representation.
   */
  def codec: ThriftStructCodec[T]

  /**
   * The Class object for the ThriftStructCodec subclass.
   */
  def codecClass: Class[_]

  /**
   * The fully qualified name of the ThriftStruct subclass.
   */
  def structClassName: String

  /**
   * Gets the unqualified name of the struct.
   */
  def structName: String

  /**
   * The Class object for ThriftStruct subclass.
   *
   * For a union, this is the parent trait of all branches for the union.
   */
  def structClass: Class[T]

  /**
   * A Seq of ThriftStructFields representing the fields defined in the ThriftStruct.
   *
   * For unions, this will return an empty Seq.
   */
  def fields: Seq[ThriftStructField[T]]

  /**
   * For non-unions, will return its [[ThriftStructFieldInfo ThriftStructFieldInfos]].
   *
   * For unions, will return an empty `Seq`.
   */
  def fieldInfos: Seq[ThriftStructFieldInfo]

  /**
   * For unions, will return its [[ThriftUnionFieldInfo ThriftUnionFieldInfos]].
   *
   * For non-unions, will return an empty `Seq`.
   */
  def unionFields: Seq[ThriftUnionFieldInfo[ThriftUnion with ThriftStruct, _]]

  /**
   * Parsed annotations at the struct or union level. Left hand side of equal sign is the key,
   * right side is the value.
   */
  def structAnnotations: Map[String, String]
}

object ThriftStructMetaData {

  /**
   * Constructs an implementation of [[ThriftStructMetaData]] that uses reflection
   * to discover field and annotation information.
   */
  def apply[T <: ThriftStruct](codec: ThriftStructCodec[T]): ThriftStructMetaData[T] =
    new ReflectionThriftStructMetaData(codec)

  /**
   * Constructs an implementation of [[ThriftStructMetaData]] that uses the passed in
   * arguments for field and annotation information.
   */
  def apply[T <: ThriftStruct](
    codec: ThriftStructCodec[T],
    fields: Seq[ThriftStructField[T]],
    fieldInfos: Seq[ThriftStructFieldInfo],
    unionFields: Seq[ThriftUnionFieldInfo[ThriftUnion with ThriftStruct, _]],
    structAnnotations: Map[String, String]
  ): ThriftStructMetaData[T] =
    new ConcreteThriftStructMetaData(codec, fields, fieldInfos, unionFields, structAnnotations)

}
