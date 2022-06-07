package com.twitter.scrooge

import com.twitter.scrooge.thrift_validation.ThriftConstraintValidator

object EmptyAnnotationValueConstraintValidator extends ThriftConstraintValidator[String, String] {

  /** Annotation value is not required for this constraint validator. */
  override def annotationClass: Class[String] = classOf[String]

  override def fieldClass: Class[String] = classOf[String]

  override def violationMessage(
    obj: String,
    annotation: String
  ): String = s"The value $obj doesn't start with a"

  /** Return true as long as the given `obj` starts with "a". */
  override def isValid(
    obj: String,
    annotation: String
  ): Boolean = obj.startsWith("a")
}
