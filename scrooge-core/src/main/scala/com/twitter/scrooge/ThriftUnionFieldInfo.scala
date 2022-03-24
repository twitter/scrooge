package com.twitter.scrooge

import scala.reflect.ClassTag

/**
 * Field information to be embedded in a generated union's companion class.
 * @param structFieldInfo Nested [[ThriftStructFieldInfo]] containing additional details about union
 *                        field
 * @param fieldUnapply The unapply method on the companion object of the union field class. Used to
 *                     easily extract the field value from an instance of the union field class.
 * @tparam UnionFieldType The type of the union field represented by this class
 * @tparam ContainedType The type of the value contained in the union field represented by this
 *                       class
 */
final class ThriftUnionFieldInfo[UnionFieldType <: ThriftUnion: ClassTag, ContainedType: ClassTag](
  val structFieldInfo: ThriftStructFieldInfo,
  fieldUnapply: UnionFieldType => scala.Option[ContainedType]) {

  /**
   * Class tag for the class representing this union field; useful for reflection-related tasks
   */
  val fieldClassTag: ClassTag[UnionFieldType] = implicitly[ClassTag[UnionFieldType]]

  private[this] val containedClassTag: ClassTag[ContainedType] = implicitly[ClassTag[ContainedType]]

  /**
   * Extracts the value contained within an instance of this union's class
   * @param field The field instance from which to extract the contained value
   * @return The extracted value
   */
  def fieldValue(field: ThriftUnion): ContainedType = {
    fieldUnapply(field.asInstanceOf[UnionFieldType]).getOrElse {
      throw new IllegalStateException(
        s"Mismatch between UnionFieldType $fieldClassTag and ContainedType $containedClassTag"
      )
    }
  }
}
