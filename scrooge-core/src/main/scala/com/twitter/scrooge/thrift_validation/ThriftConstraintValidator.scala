package com.twitter.scrooge.thrift_validation

/**
 * The trait to define a ThriftConstraintValidator, this is used
 * in [[ThriftValidator]]to validate annotations on Thrift IDL.
 *
 * @tparam T The type of the field to be validated.
 * @tparam A The type of the annotation value to be validated against.
 *
 * @note [[A]] must be a super type of [[Int]], [[Long]], [[Double]],
 *       [[Short]], [[Byte]], or [[String]].
 */
trait ThriftConstraintValidator[
  T,
  A >: Int with Long with Double with Short with Byte with String] {

  /**
   * @return the annotation class if annotation value is required,
   * e.g. validation.max = "1". Otherwise return None.
   */
  def annotationClazz: Class[A]

  /** @return the class of the field where the annotation applies to. */
  def fieldClazz: Class[T]

  /** Define a violation message if the given `obj` failed validation */
  def violationMessage(obj: T, annotation: A): String

  /** The validation on value `obj` against constraint `annotation` passed. */
  def isValid(obj: T, annotation: A): Boolean
}
