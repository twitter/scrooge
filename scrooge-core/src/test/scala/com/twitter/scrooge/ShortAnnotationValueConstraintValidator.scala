package com.twitter.scrooge

import com.twitter.scrooge.thrift_validation.ThriftConstraintValidator

object ShortAnnotationValueConstraintValidator extends ThriftConstraintValidator[Short, Short] {

  /**
   * The IDL annotation for this constraint validator is validation.shortEquals = "7"
   * where the annotation value is an integer.
   */
  override def annotationClass: Class[Short] = classOf[Short]

  override def fieldClass: Class[Short] = classOf[Short]

  override def violationMessage(
    obj: Short,
    annotation: Short
  ): String = s"$obj does not equal to $annotation."

  /** return true if the value of `obj` == the value of `annotation`. */
  override def isValid(
    obj: Short,
    annotation: Short
  ): Boolean = obj == annotation
}
