package com.twitter.scrooge.thrift_validation

import com.twitter.scrooge.thrift_validation.ThriftValidator.DefaultAnnotationKeys
import scala.collection.JavaConverters
import scala.collection.mutable

object ThriftValidator {
  val DefaultAnnotationKeys: Set[String] = Set(
    "validation.assertFalse",
    "validation.assertTrue",
    "validation.countryCode",
    "validation.EAN",
    "validation.email",
    "validation.UUID",
    "validation.ISBN",
    "validation.length.min",
    "validation.length.max",
    "validation.max",
    "validation.min",
    "validation.negative",
    "validation.negativeOrZero",
    "validation.notEmpty",
    "validation.positive",
    "validation.positiveOrZero",
    "validation.size.min",
    "validation.size.max"
  )
}

/**
 * Implement this class to define a validator with custom validations.
 */
abstract class ThriftValidator extends BaseValidator {
  // Do not allow to override default annotations since we have a different
  // set of rules for default annotations and they are done before performing
  // custom validations.
  require(customAnnotations.keySet.intersect(DefaultAnnotationKeys).isEmpty)

  /**
   * A map of String annotations to [[ThriftConstraintValidator]].
   * @note Must override this method to define all custom validations
   *       in an implementation of [[ThriftValidator]].
   *
   * @note There is a Java helper `toScalaMap`, which takes a [[java.util.Map]]
   *       and return a [[scala.collection.immutable.Map]] for `customAnnotations`.
   */
  def customAnnotations: Map[String, ThriftConstraintValidator[_, _]]

  /**
   * A helper method that converts a [[java.util.Map]]
   * to a [[scala.collection.immutable.Map]]
   */
  protected final def toScalaMap(
    javaMap: java.util.Map[String, ThriftConstraintValidator[_, _]]
  ): Map[String, ThriftConstraintValidator[_, _]] = {
    JavaConverters.mapAsScalaMapConverter(javaMap).asScala.toMap
  }

  /** All custom annotations defined in this validator. */
  def annotations: Set[String] = customAnnotations.keySet

  /**
   * Validate a field value against its annotation.
   *
   * @param fieldName        The name of the field.
   * @param fieldValue       The value of the field to be validated.
   * @param fieldAnnotations The annotation name and its value set on
   *                         the field in Thrift IDL.
   * @tparam T The type of the field.
   *
   * @return A set of [[ThriftValidationViolation]]s for violated
   *         constraints. Return an empty set if all validations
   *         passed.
   *
   * @note See a Java-friendly version of `validateField` that takes
   *       fieldAnnotations as a [[java.util.Map]], and return a set
   *       of [[ThriftValidationViolation]]s as [[java.util.Set]] in
   *       [[BaseValidator.validateField()]].
   */
  def validateField[T](
    fieldName: String,
    fieldValue: T,
    fieldAnnotations: Map[String, String]
  ): Set[ThriftValidationViolation] = {
    val violations: mutable.Set[ThriftValidationViolation] = mutable.Set.empty
    for ((annotationKey, annotationValue) <- fieldAnnotations) {
      // skip validations if an annotation is not recognized. This is in order
      // not to break the services when they use annotations for other purposes
      // other than Thrift Validations.
      customAnnotations.get(annotationKey) match {
        case Some(constraintValidator) =>
          val clazz = constraintValidator.annotationClass
          val violation = {
            if (clazz == classOf[java.lang.Long] || clazz == classOf[Long]) {
              validateCustomConstraint[T, Long](
                fieldName,
                fieldValue,
                annotationValue.toLong,
                constraintValidator.asInstanceOf[ThriftConstraintValidator[T, Long]])
            } else if (clazz == classOf[java.lang.Integer] || clazz == classOf[Int]) {
              validateCustomConstraint[T, Int](
                fieldName,
                fieldValue,
                annotationValue.toInt,
                constraintValidator.asInstanceOf[ThriftConstraintValidator[T, Int]])
            } else if (clazz == classOf[java.lang.Double] || clazz == classOf[Double]) {
              validateCustomConstraint[T, Double](
                fieldName,
                fieldValue,
                annotationValue.toDouble,
                constraintValidator.asInstanceOf[ThriftConstraintValidator[T, Double]])
            } else if (clazz == classOf[java.lang.Short] || clazz == classOf[Short]) {
              validateCustomConstraint[T, Short](
                fieldName,
                fieldValue,
                annotationValue.toShort,
                constraintValidator.asInstanceOf[ThriftConstraintValidator[T, Short]])
            } else if (clazz == classOf[java.lang.Byte] || clazz == classOf[Byte]) {
              validateCustomConstraint[T, Byte](
                fieldName,
                fieldValue,
                annotationValue.toByte,
                constraintValidator.asInstanceOf[ThriftConstraintValidator[T, Byte]])
            } else if (clazz == classOf[java.lang.String] || clazz == classOf[String]) {
              validateCustomConstraint[T, String](
                fieldName,
                fieldValue,
                annotationValue,
                constraintValidator.asInstanceOf[ThriftConstraintValidator[T, String]])
            } else {
              throw new IllegalArgumentException(
                s"The annotation with value $annotationValue's type is $clazz, $clazz is not among " +
                  s"the supported types Int, Long, Double, Short, Byte, and String.")
            }
          }
          violations ++= violation
        case None =>
        // skip validations if an annotation is not recognized. This is in order
        // not to break the services when they use annotations for other purposes
        // other than Thrift Validations.
      }
    }
    violations.toSet
  }

  private def validateCustomConstraint[
    T,
    A >: Int with Long with Double with Short with Byte with String
  ](
    fieldName: String,
    fieldValue: T,
    annotationValue: A,
    constraintValidator: ThriftConstraintValidator[T, A]
  ): Set[ThriftValidationViolation] = {
    if (constraintValidator.isValid(fieldValue, annotationValue))
      Set.empty
    else
      Set(
        ThriftValidationViolation(
          fieldName,
          fieldValue,
          constraintValidator.violationMessage(fieldValue, annotationValue)))
  }
}
