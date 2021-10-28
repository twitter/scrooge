package com.twitter.scrooge.validation

import com.twitter.scrooge.ThriftValidator

object CustomValidator {
  val validator = ThriftValidator
    .newBuilder()
    .withConstraints(Map(
      "validation.testStartWithA" -> StartWithAConstraintValidator,
      "validation.testUserId" -> TestUserIdConstraintValidator,
      "validation.testScreenName" -> ScreenNameConstraintValidator
    ))
    .build()
  new JCustomValidator().getThriftValidator
}

object StartWithAConstraintValidator extends ThriftConstraintValidator[String, String] {

  /** Annotation value is not required for this constraint validator. */
  override def annotationClazz: Class[String] = classOf[String]

  override def violationMessage(
    obj: String,
    annotation: String
  ): String = "must start with a"

  /** Return true as long as the given `obj` starts with "a". */
  override def isValid(
    obj: String,
    annotation: String
  ): Boolean = obj.startsWith("a")
}

object TestUserIdConstraintValidator extends ThriftConstraintValidator[Long, String] {

  /** Annotation value is not required for this constraint validator. */
  override def annotationClazz: Class[String] = classOf[String]

  override def violationMessage(obj: Long, annotation: String): String = "invalid user id"

  /** A valid test user id is between 11111 and 22222 */
  override def isValid(obj: Long, annotation: String): Boolean = obj > 11111 && obj < 22222
}

object ScreenNameConstraintValidator extends ThriftConstraintValidator[String, String] {

  /** Annotation value is not required for this constraint validator. */
  override def annotationClazz: Class[String] = classOf[String]

  override def violationMessage(
    obj: String,
    annotation: String
  ): String = "invalid user screen name"

  /** A valid user screen nmae must start with "@" */
  override def isValid(
    obj: String,
    annotation: String
  ): Boolean = obj.startsWith("@")
}
