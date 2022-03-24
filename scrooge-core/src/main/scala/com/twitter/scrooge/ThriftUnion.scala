package com.twitter.scrooge

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
