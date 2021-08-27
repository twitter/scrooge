package com.twitter.scrooge

import com.twitter.scrooge.ThriftValidator.DefaultConstraints
import com.twitter.scrooge.validation.{ThriftConstraintValidator, ThriftValidationViolation}
import com.twitter.util.validation.ScalaValidator
import com.twitter.util.validation.constraints._
import jakarta.validation.constraints._
import java.lang.annotation.Annotation
import org.hibernate.validator.constraints._
import scala.collection.mutable
import scala.jdk.CollectionConverters._

object ThriftValidator {

  private case class ConstraintMetadata(
    annotationType: Class[_ <: Annotation],
    annotationValues: Option[String] = None)
  // A mapping from Thrift String annotation to Jakarta bean
  // annotation and its metadata. This includes some built-in
  // validation constraints in util-validation, and is used to call
  // [[ScalaValidator.validateFieldValue]] to validate each field
  // defined in the Thrift IDL.
  private val DefaultConstraints: Map[String, ConstraintMetadata] = Map(
    "validation.countryCode" -> ConstraintMetadata(classOf[CountryCode]),
    "validation.UUID" -> ConstraintMetadata(classOf[UUID]),
    "validation.assertFalse" -> ConstraintMetadata(classOf[AssertFalse]),
    "validation.assertTrue" -> ConstraintMetadata(classOf[AssertTrue]),
    "validation.max" -> ConstraintMetadata(classOf[Max], Some("value")),
    "validation.notEmpty" -> ConstraintMetadata(classOf[jakarta.validation.constraints.NotEmpty]),
    "validation.min" -> ConstraintMetadata(classOf[Min], Some("value")),
    "validation.size.min" -> ConstraintMetadata(classOf[Size], Some("min")),
    "validation.size.max" -> ConstraintMetadata(classOf[Size], Some("max")),
    "validation.email" -> ConstraintMetadata(classOf[jakarta.validation.constraints.Email]),
    "validation.negative" -> ConstraintMetadata(classOf[Negative]),
    "validation.negativeOrZero" -> ConstraintMetadata(classOf[NegativeOrZero]),
    "validation.positive" -> ConstraintMetadata(classOf[Positive]),
    "validation.positiveOrZero" -> ConstraintMetadata(classOf[PositiveOrZero]),
    "validation.EAN" -> ConstraintMetadata(classOf[EAN]),
    "validation.ISBN" -> ConstraintMetadata(classOf[ISBN]),
    "validation.length.min" -> ConstraintMetadata(classOf[Length], Some("min")),
    "validation.length.max" -> ConstraintMetadata(classOf[Length], Some("max"))
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
     */
    def withConstraints(constraints: Map[String, ThriftConstraintValidator[_, _]]): Builder =
      Builder(constraints)

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
    val constraints: mutable.Map[Class[_ <: Annotation], Map[String, Any]] = mutable.Map.empty

    for ((annotationKey, annotationValue) <- fieldAnnotations) {
      // validation.size and validation.length work on collections,
      // which will be evaluated in the `collectionConstraints`
      if (!annotationKey.startsWith("validation.size") && !annotationKey.startsWith(
          "validation.length")) {
        val constraintMetadata = DefaultConstraints(annotationKey)
        // For annotations that require a value, e.g. max = "10", the type of
        // the annotation value should match the type of the field, this is
        // enforced at compile time.
        val constraintsAnnotation: Map[String, Any] = {
          constraintMetadata.annotationValues match {
            case Some(annotationName) =>
              Map(annotationName -> castString(annotationValue, fieldValue.getClass))
            case None => Map.empty[String, Any]
          }
        }
        constraints += (constraintMetadata.annotationType -> constraintsAnnotation)
      }
    }

    // validation.size and validation.length are evaluated separately
    // since they both apply to collections which we treat differently.
    constraints ++= collectionConstraints(
      fieldAnnotations,
      "validation.size.min",
      "validation.size.max")
    constraints ++= collectionConstraints(
      fieldAnnotations,
      "validation.length.min",
      "validation.length.max")

    underlying
      .validateFieldValue(constraints.toMap, fieldName, fieldValue)
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
          val castedAnnotationValue =
            castString(annotationValue, constraintValidator.annotationClazz)
          val violation = castedAnnotationValue match {
            case _: Int =>
              validateCustomConstraint[T, Int](
                fieldName,
                fieldValue,
                castedAnnotationValue.asInstanceOf[Int],
                constraintValidator.asInstanceOf[ThriftConstraintValidator[T, Int]])
            case _: Long =>
              validateCustomConstraint[T, Long](
                fieldName,
                fieldValue,
                castedAnnotationValue.asInstanceOf[Long],
                constraintValidator.asInstanceOf[ThriftConstraintValidator[T, Long]])
            case _: Double =>
              validateCustomConstraint[T, Double](
                fieldName,
                fieldValue,
                castedAnnotationValue.asInstanceOf[Double],
                constraintValidator.asInstanceOf[ThriftConstraintValidator[T, Double]])
            case _: Byte =>
              validateCustomConstraint[T, Byte](
                fieldName,
                fieldValue,
                castedAnnotationValue.asInstanceOf[Byte],
                constraintValidator.asInstanceOf[ThriftConstraintValidator[T, Byte]])
            case _: Short =>
              validateCustomConstraint[T, Short](
                fieldName,
                fieldValue,
                castedAnnotationValue.asInstanceOf[Short],
                constraintValidator.asInstanceOf[ThriftConstraintValidator[T, Short]])
            case _: String =>
              validateCustomConstraint[T, String](
                fieldName,
                fieldValue,
                castedAnnotationValue.asInstanceOf[String],
                constraintValidator.asInstanceOf[ThriftConstraintValidator[T, String]])
            case _ => Set.empty[ThriftValidationViolation]
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

  // cast the given string to a value of type `A`.
  private def castString[A](string: String, clazz: Class[A]): A =
    if (clazz == classOf[java.lang.Long] || clazz == classOf[Long])
      string.toLong.asInstanceOf[A]
    else if (clazz == classOf[java.lang.Integer] || clazz == classOf[Int])
      string.toInt.asInstanceOf[A]
    else if (clazz == classOf[java.lang.Double] || clazz == classOf[Double])
      string.toDouble.asInstanceOf[A]
    else if (clazz == classOf[java.lang.Short] || clazz == classOf[Short])
      string.toShort.asInstanceOf[A]
    else if (clazz == classOf[java.lang.Byte] || clazz == classOf[Byte])
      string.toByte.asInstanceOf[A]
    else if (clazz == classOf[java.lang.String] || clazz == classOf[String])
      string.asInstanceOf[A]
    else
      throw new IllegalArgumentException(
        s"The annotation with value $string's type is $clazz, $clazz is not among " +
          s"the supported types Int, Long, Double, Short, Byte, and String.")

  // some annotations applies to collections
  // require setting up a min or/and a max value.
  // eg. "validation.size", "validation.length".
  private def collectionConstraints(
    fieldAnnotations: Map[String, String],
    minConstraint: String,
    maxConstraint: String
  ): Map[Class[_ <: Annotation], Map[String, Any]] =
    (fieldAnnotations.get(minConstraint), fieldAnnotations.get(maxConstraint)) match {
      case (Some(min), Some(max)) =>
        Map(
          DefaultConstraints(minConstraint).annotationType -> Map(
            "min" -> min.toInt,
            "max" -> max.toInt))
      case (Some(min), _) =>
        Map(DefaultConstraints(minConstraint).annotationType -> Map("min" -> min.toInt))
      case (_, Some(max)) =>
        Map(DefaultConstraints(maxConstraint).annotationType -> Map("max" -> max.toInt))
      case _ => Map.empty[Class[_ <: Annotation], Map[String, Any]]
    }
}
