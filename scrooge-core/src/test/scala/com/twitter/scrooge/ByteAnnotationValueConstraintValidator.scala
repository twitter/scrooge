package com.twitter.scrooge

import com.twitter.scrooge.validation.ThriftConstraintValidator

object ByteAnnotationValueConstraintValidator extends ThriftConstraintValidator[Byte, Byte] {

  /**
   * The IDL annotation for this constraint validator is validation.byteEquals = "7"
   * where the annotation value is an integer.
   */
  override def annotationClazz: Class[Byte] = classOf[Byte]

  override def violationMessage(
    obj: Byte,
    annotation: Byte
  ): String = s"$obj does not equal to $annotation."

  /** return true if the value of `obj` == the value of `annotation`. */
  override def isValid(
    obj: Byte,
    annotation: Byte
  ): Boolean = obj == annotation
}
