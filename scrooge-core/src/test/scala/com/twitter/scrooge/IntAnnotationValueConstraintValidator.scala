package com.twitter.scrooge

import com.twitter.scrooge.thrift_validation.ThriftConstraintValidator

object IntAnnotationValueConstraintValidator extends ThriftConstraintValidator[String, Int] {

  /**
   * The IDL annotation for this constraint validator is validation.charLengthInt = "7"
   * where the annotation value is an integer.
   */
  override def annotationClass: Class[Int] = classOf[Int]

  override def fieldClass: Class[String] = classOf[String]

  override def violationMessage(
    obj: String,
    annotation: Int
  ): String = s"The length of the string $obj is not $annotation."

  /** return true if the given length of `obj` equals to the value of `annotation`. */
  override def isValid(
    obj: String,
    annotation: Int
  ): Boolean = obj.length == annotation
}
