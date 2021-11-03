package com.twitter.scrooge

import com.twitter.scrooge.thrift_validation.ThriftConstraintValidator

/**
 * A custom constraint validator that will always fail any validations
 * with a [[RuntimeException]].
 */
object InvalidConstraintValidator extends ThriftConstraintValidator[Int, String] {

  override def annotationClazz: Class[String] = classOf[String]

  override def fieldClazz: Class[Int] = classOf[Int]

  override def violationMessage(
    obj: Int,
    annotation: String
  ): String = "always fail"

  /** Always return a [[RuntimeException]] */
  override def isValid(obj: Int, annotation: String): Boolean =
    throw new RuntimeException(s"The validation on $obj failed validation.")
}
