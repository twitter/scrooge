package com.twitter.scrooge.thrift_validation

import scala.collection.JavaConverters._

/**
 * An interface to define APIs for default validations (depend on
 * `com.twitter.util.validation.ScalaValidator`) and custom
 * validations.
 */
abstract class BaseValidator {

  /** All annotations defined in this validator. */
  def annotations: Set[String]

  /**
   * Check if a given [[annotation]] is already defined.
   *
   * @param annotation the String annotation defined on IDL.
   * @return true if the [[annotation]] is already defined.
   */
  def annotationIsDefined(annotation: String): Boolean = annotations.contains(annotation)

  /**
   * Validate a field value against its annotation.
   * @param fieldName The name of the field.
   * @param fieldValue The value of the field to be validated.
   * @param fieldAnnotations The annotation name and its value set on
   *                         the field in Thrift IDL.
   * @tparam T The type of the field.
   * @return A set of [[ThriftValidationViolation]]s for violated
   *         constraints. Return an empty set if all validations
   *         passed.
   * @note See a Java-friendly version of [[validateField]] that
   *       takes fieldAnnotations as a [[java.util.Map]], and return
   *       a set of [[ThriftValidationViolation]]s as
   *       [[java.util.Set]].
   */
  def validateField[T](
    fieldName: String,
    fieldValue: T,
    fieldAnnotations: Map[String, String]
  ): Set[ThriftValidationViolation]

  /**
   * A Java-friendly version of [[validateField]], that takes
   * fieldAnnotations as a [[java.util.Map]], and return a set
   * of [[ThriftValidationViolation]]s as [[java.util.Set]].
   */
  def validateField[T](
    fieldName: String,
    fieldValue: T,
    fieldAnnotations: java.util.Map[String, String]
  ): java.util.Set[ThriftValidationViolation] = {
    validateField(fieldName, fieldValue, fieldAnnotations.asScala.toMap).asJava
  }
}
