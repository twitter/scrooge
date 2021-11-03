package com.twitter.scrooge

import com.twitter.scrooge.UtilValidator.DefaultAnnotations
import com.twitter.scrooge.UtilValidator.scalaValidator
import com.twitter.scrooge.thrift_validation.BaseValidator
import com.twitter.scrooge.thrift_validation.ThriftValidationViolation
import com.twitter.util.validation.ScalaValidator
import com.twitter.util.validation.constraints._
import jakarta.validation.constraints._
import java.lang.annotation.Annotation
import org.hibernate.validator.constraints._

object UtilValidator {

  /**
   * A mapping from Thrift String annotation to Jakarta bean
   * annotation. This includes some built-in validation constraints
   * in util-validation, and is used to call
   * [[ScalaValidator.validateFieldValue]] to validate each field
   * defined in the Thrift IDL.
   */
  val DefaultAnnotations: Map[String, Class[_ <: Annotation]] = Map(
    "validation.assertFalse" -> classOf[AssertFalse],
    "validation.assertTrue" -> classOf[AssertTrue],
    "validation.countryCode" -> classOf[CountryCode],
    "validation.EAN" -> classOf[EAN],
    "validation.email" -> classOf[jakarta.validation.constraints.Email],
    "validation.UUID" -> classOf[UUID],
    "validation.ISBN" -> classOf[ISBN],
    "validation.length.min" -> classOf[Length],
    "validation.length.max" -> classOf[Length],
    "validation.max" -> classOf[Max],
    "validation.min" -> classOf[Min],
    "validation.negative" -> classOf[Negative],
    "validation.negativeOrZero" -> classOf[NegativeOrZero],
    "validation.notEmpty" -> classOf[jakarta.validation.constraints.NotEmpty],
    "validation.positive" -> classOf[Positive],
    "validation.positiveOrZero" -> classOf[PositiveOrZero],
    "validation.size.min" -> classOf[Size],
    "validation.size.max" -> classOf[Size],
  )

  /** A singleton [[ScalaValidator]] to perform default validations. */
  val scalaValidator: ScalaValidator = ScalaValidator()

  def apply(): UtilValidator = new UtilValidator()
}

/**
 * An implementation of [[BaseValidator]] that leverage
 * [[com.twitter.util.validation.ScalaValidator]] to perform
 * validations against `DefaultAnnotations`.
 * @note This class is only used for Scrooge internally.
 */
final class UtilValidator extends BaseValidator {

  override def annotations: Set[String] = DefaultAnnotations.keySet

  // validate each field value for default annotations defined on
  // this field.
  override def validateField[T](
    fieldName: String,
    fieldValue: T,
    fieldAnnotations: Map[String, String]
  ): Set[ThriftValidationViolation] = {
    val constraints = fieldAnnotations.flatMap {
      case (annotationKey, annotationValue) =>
        // 4 default annotations require an annotation value:
        // "validation.size": applies to container types, requires an annotation type `int`.
        // "validation.length": applies to String, requires an annotation type `int`.
        // "validation.min": applies to numeric types, requires an annotation type `long`.
        // "validation.max": apply to numeric types, requires an annotation type `long`.
        // the annotation types are checked and enforced at compile time.
        if (annotationKey.startsWith("validation.size")) {
          sizeAndLengthConstraints(fieldAnnotations, "validation.size.min", "validation.size.max")
        } else if (annotationKey.startsWith("validation.length")) {
          sizeAndLengthConstraints(
            fieldAnnotations,
            "validation.length.min",
            "validation.length.max")
        } else if (annotationKey.startsWith("validation.min")) {
          // it is safe to call `toLong` since the annotation value
          // types are checked and enforced at compile time.
          Map[Class[_ <: Annotation], Map[String, Long]](
            classOf[Min] -> Map("value" -> annotationValue.toLong))
        } else if (annotationKey.startsWith("validation.max")) {
          // it is safe to call `toLong` since the annotation value
          // types are checked and enforced at compile time.
          Map[Class[_ <: Annotation], Map[String, Long]](
            classOf[Max] -> Map("value" -> annotationValue.toLong))
        } else if (annotationIsDefined(annotationKey)) {
          Map[Class[_ <: Annotation], Map[String, Any]](
            DefaultAnnotations(annotationKey) -> Map.empty[String, Any])
        } else {
          Set.empty
        }
    }

    scalaValidator
      .validateFieldValue(constraints, fieldName, fieldValue)
      .map(violation => ThriftValidationViolation(fieldName, fieldValue, violation.getMessage))
  }

  // "validation.size" and "validation.length"
  // require setting up a min or/and a max value.
  private def sizeAndLengthConstraints(
    fieldAnnotations: Map[String, String],
    minConstraint: String,
    maxConstraint: String
  ): Map[Class[_ <: Annotation], Map[String, Any]] =
    (fieldAnnotations.get(minConstraint), fieldAnnotations.get(maxConstraint)) match {
      case (Some(min), Some(max)) =>
        Map(DefaultAnnotations(minConstraint) -> Map("min" -> min.toInt, "max" -> max.toInt))
      case (Some(min), _) =>
        Map(DefaultAnnotations(minConstraint) -> Map("min" -> min.toInt))
      case (_, Some(max)) =>
        Map(DefaultAnnotations(maxConstraint) -> Map("max" -> max.toInt))
      case _ => Map.empty[Class[_ <: Annotation], Map[String, Any]]
    }
}
