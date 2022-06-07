package com.twitter.scrooge

import com.twitter.scrooge.thrift_validation.ThriftConstraintValidator

object LongAnnotationValueConstraintValidator extends ThriftConstraintValidator[Long, Long] {

  /**
   * The IDL annotation for this constraint validator is validation.longEquals = "7L"
   * where the annotation value is an integer.
   */
  override def annotationClass: Class[Long] = classOf[Long]

  override def fieldClass: Class[Long] = classOf[Long]

  override def violationMessage(
    obj: Long,
    annotation: Long
  ): String = s"$obj does not equal to $annotation."

  /** return true if the value of `obj` == the value of `annotation`. */
  override def isValid(
    obj: Long,
    annotation: Long
  ): Boolean = obj == annotation
}
