package com.twitter.scrooge

import com.twitter.util.Memoize

/**
 * Unions are tagged with this trait.
 */
trait ThriftUnion extends ThriftStruct {

  /**
   * The type of the value contained in the union field.
   *
   * For unknown union fields, this will be `Unit`.
   *
   * @see the `ContainedType` type param on [[ThriftUnionFieldInfo]]
   */
  protected type ContainedType

  /**
   * The value of this union field.
   *
   * For unknown union fields, this will be `()`, the instance of `Unit`.
   */
  def containedValue(): ContainedType

  /**
   * The [[ThriftStructFieldInfo]] for this part of union.
   *
   * Returns `None` if this represents an unknown union field.
   */
  def unionStructFieldInfo: Option[ThriftStructFieldInfo]
}

object ThriftUnion {
  private[this] val fieldInfos = Memoize.classValue { c =>
    if (!classOf[ThriftUnion].isAssignableFrom(c)) {
      throw new IllegalArgumentException(s"${c.getName} does not extend ThriftUnion")
    }
    ThriftStructMetaData
      .forStructClass(c.asInstanceOf[Class[ThriftUnion]])
      .unionFields
      .collectFirst {
        case f if f.fieldClassTag.runtimeClass == c => f.structFieldInfo
      }
  }

  /**
   * Given a specific union member class, returns the struct field info for it.
   * It returns the same value as invoking `unionStructFieldInfo` on an instance
   * of the class, without having to create an instance of it.
   * @param c the union member class
   * @return the struct field info for the class, or None
   */
  def fieldInfoForUnionClass(c: Class[_ <: ThriftUnion]): Option[ThriftStructFieldInfo] =
    fieldInfos(c)
}
