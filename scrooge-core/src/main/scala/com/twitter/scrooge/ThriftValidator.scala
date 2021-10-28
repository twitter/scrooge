package com.twitter.scrooge

import com.twitter.scrooge.ThriftValidator.DefaultConstraints
import com.twitter.scrooge.validation.ThriftConstraintValidator
import com.twitter.scrooge.validation.ThriftValidationViolation
import com.twitter.util.validation.ScalaValidator
import com.twitter.util.validation.constraints._
import jakarta.validation.constraints._
import java.lang.annotation.Annotation
import org.hibernate.validator.constraints._
import scala.collection.mutable
import scala.jdk.CollectionConverters._

object ThriftValidator {
  // A mapping from Thrift String annotation to Jakarta bean annotation.
  // This includes some built-in validation constraints in util-validation,
  // and is used to call [[ScalaValidator.validateFieldValue]] to validate
  // each field defined in the Thrift IDL.
  private val DefaultConstraints: Map[String, Class[_ <: Annotation]] = Map(
    "validation.countryCode" -> classOf[CountryCode],
    "validation.UUID" -> classOf[UUID],
    "validation.assertFalse" -> classOf[AssertFalse],
    "validation.assertTrue" -> classOf[AssertTrue],
    "validation.max" -> classOf[Max],
    "validation.notEmpty" -> classOf[jakarta.validation.constraints.NotEmpty],
    "validation.min" -> classOf[Min],
    "validation.size.min" -> classOf[Size],
    "validation.size.max" -> classOf[Size],
    "validation.email" -> classOf[jakarta.validation.constraints.Email],
    "validation.negative" -> classOf[Negative],
    "validation.negativeOrZero" -> classOf[NegativeOrZero],
    "validation.positive" -> classOf[Positive],
    "validation.positiveOrZero" -> classOf[PositiveOrZero],
    "validation.EAN" -> classOf[EAN],
    "validation.ISBN" -> classOf[ISBN],
    "validation.length.min" -> classOf[Length],
    "validation.length.max" -> classOf[Length]
  )

  /**
   * @return a [[ThriftValidator]] with default constraint validations.
   */
  def apply(): ThriftValidator = newBuilder().build()

  /**
   * @return a [[Builder]] to build a [[ThriftValidator]] with custom
   *         constraint validations.
   */
  def newBuilder(): Builder = Builder()

  /**
   * Builder style to create a [[ThriftValidator]].
   *
   * @param customConstraints a map of constraint validation name to
   *                          its [[ThriftConstraintValidator]].
   *
   * {{{
   *   val builder =
   *     ThriftValidator.newBuilder()
   *       .withConstraints(
   *         Map(
   *           "validation.invalid" -> InvalidConstraintValidator,
   *           "validation.emptyAnnotation" -> EmptyAnnotationValueConstraintValidator,
   *           "validation.requiredAnnotation" -> RequiredAnnotationValueConstraintValidator
   *         )
   *       ).build()
   *
   *   val thriftValidator: ThriftValidator = builder.build
   * }}}
   */
  case class Builder private[scrooge] (
    customConstraints: Map[String, ThriftConstraintValidator[_, _]] = Map.empty) {

    /**
     * Create a [[Builder]] with [[customConstraints]].
     *
     * @param constraints a map of constraint validation name to
     *                    [[ThriftConstraintValidator]].
     *
     * @return a [[Builder]] with embedded [[customConstraints]].
     *
     * @note If any of the provided [[customConstraints]] is already
     *       defined, an [[IllegalArgumentException]] will be thrown.
     * @see [[withConstraints(java.util.Map)]] for a Java-friendly
     *      version.
     */
    def withConstraints(constraints: Map[String, ThriftConstraintValidator[_, _]]): Builder =
      Builder(constraints)

    /**
     * Java-friendly API to create a [[Builder]] with [[customConstraints]].
     *
     * @param constraints a [[java.util.Map]] of constraint validation name to
     *                    [[ThriftConstraintValidator]].
     * @return a [[Builder]] with embedded [[customConstraints]].
     * @see [[withConstraints]] for the Scala API.
     */
    def withConstraints(
      constraints: java.util.Map[String, ThriftConstraintValidator[_, _]]
    ): Builder =
      withConstraints(constraints.asScala.toMap)

    /**
     * Create a [[ThriftValidator]] with given [[customConstraints]].
     *
     * @return a [[ThriftValidator]] with given [[customConstraints]].
     */
    def build(): ThriftValidator = new ThriftValidator(customConstraints, ScalaValidator())
  }
}

/**
 * This class is for Thrift to perform per field validations for
 * String annotations.
 *
 * @param customAnnotations a map of String annotations to
 *                          [[ThriftConstraintValidator]].
 * @param underlying a [[ScalaValidator]] to be invoked for per field
 *                   validations for default constraint validations.
 * @throws IllegalArgumentException if any of the annotation names
 *                                  from [[customAnnotations]] is
 *                                  already defined by the framework.
 */
class ThriftValidator(
  customAnnotations: Map[String, ThriftConstraintValidator[_, _]],
  underlying: ScalaValidator) {

  // Overriding default annotations is not allowed
  require(customAnnotations.keySet.intersect(DefaultConstraints.keySet).isEmpty)

  /**
   * Return all annotations defined for the validator, including
   * default and custom annotations.
   */
  val annotations: Set[String] = DefaultConstraints.keySet ++ customAnnotations.keySet

  /**
   * Check if a given [[annotation]] is already defined in this [[ThriftValidator]].
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
   * @note See a Java-friendly version of [[validateField]] that takes
   *       fieldAnnotations as a [[java.util.Map]], and return a set of
   *       [[ThriftValidationViolation]]s as [[java.util.Set]].
   */
  def validateField[T](
    fieldName: String,
    fieldValue: T,
    fieldAnnotations: Map[String, String]
  ): Set[ThriftValidationViolation] = {
    val (defaultAnnotations, customAnnotations) =
      fieldAnnotations.partition { case (k, _) => DefaultConstraints.contains(k) }
    validateDefaultConstraints[T](fieldName, fieldValue, defaultAnnotations) ++
      validateCustomConstraints[T](fieldName, fieldValue, customAnnotations)
  }

  /**
   * A Java-friendly version of [[validateField]], that takes fieldAnnotations
   * as a [[java.util.Map]], and return a set of [[ThriftValidationViolation]]s
   * as [[java.util.Set]].
   */
  def validateField[T](
    fieldName: String,
    fieldValue: T,
    fieldAnnotations: java.util.Map[String, String]
  ): java.util.Set[ThriftValidationViolation] = {
    validateField(fieldName, fieldValue, fieldAnnotations.asScala.toMap).asJava
  }

  // For default constraint validations, we invoke ScalaValidator.validateFieldValue
  private def validateDefaultConstraints[T](
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
        } else {
          Map[Class[_ <: Annotation], Map[String, Any]](
            DefaultConstraints(annotationKey) -> Map.empty[String, Any])
        }
    }

    underlying
      .validateFieldValue(constraints, fieldName, fieldValue)
      .map(violation => ThriftValidationViolation(fieldName, fieldValue, violation.getMessage))
  }

  private def validateCustomConstraints[T](
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
          val clazz = constraintValidator.annotationClazz
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

  // "validation.size" and "validation.length"
  // require setting up a min or/and a max value.
  private def sizeAndLengthConstraints(
    fieldAnnotations: Map[String, String],
    minConstraint: String,
    maxConstraint: String
  ): Map[Class[_ <: Annotation], Map[String, Any]] =
    (fieldAnnotations.get(minConstraint), fieldAnnotations.get(maxConstraint)) match {
      case (Some(min), Some(max)) =>
        Map(DefaultConstraints(minConstraint) -> Map("min" -> min.toInt, "max" -> max.toInt))
      case (Some(min), _) =>
        Map(DefaultConstraints(minConstraint) -> Map("min" -> min.toInt))
      case (_, Some(max)) =>
        Map(DefaultConstraints(maxConstraint) -> Map("max" -> max.toInt))
      case _ => Map.empty[Class[_ <: Annotation], Map[String, Any]]
    }
}
