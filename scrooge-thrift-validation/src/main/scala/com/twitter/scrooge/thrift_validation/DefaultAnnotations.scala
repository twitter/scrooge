package com.twitter.scrooge.thrift_validation

import com.twitter.util.validation.constraints._
import jakarta.validation.constraints._
import java.lang.annotation.Annotation
import org.hibernate.validator.constraints._

object DefaultAnnotations {

  /**
   * The metadata for default annotations. This includes the required
   * class type for the annotation value, the list of field classes
   * that the annotation is legal to apply to, and the underlying Jakarta
   * bean annotation.
   */
  case class AnnotationMetaData(
    // annotation class, None if annotation value is not required.
    // e.g. validation.notEmpty = ""
    annotationClazz: Option[Class[_]],
    // the list of field classes that the annotation is legal to
    // apply to.
    fieldClazz: Set[Class[_]],
    // default validations depends on util ScalaValidator, which
    // depends on Jakarta annotations.
    jakartaAnnotation: Class[_ <: Annotation])

  // we need both Java and Scala types here since users might define
  // their `ThriftConstraintValidator` in either Scala or Java.
  private val NumericClasses: Set[Class[_]] =
    Set(
      classOf[java.lang.Integer],
      classOf[java.lang.Long],
      classOf[java.lang.Byte],
      classOf[java.lang.Short],
      classOf[java.lang.Double],
      classOf[Int],
      classOf[Long],
      classOf[Byte],
      classOf[Short],
      classOf[Double]
    )

  // Only need Scala collection here since this is only defined for
  // default annotations, which is defined in Scala in `ScalaValidator`.
  private val BaseCollectionClasses: Set[Class[_]] =
    Set(classOf[Seq[_]], classOf[Set[_]], classOf[Map[_, _]])

  /**
   * A mapping from Thrift String annotation to [[AnnotationMetaData]].
   */
  val metadata: Map[String, AnnotationMetaData] = Map(
    "validation.assertFalse" -> AnnotationMetaData(
      None,
      Set(classOf[java.lang.Boolean], classOf[Boolean]),
      classOf[AssertFalse]),
    "validation.assertTrue" -> AnnotationMetaData(
      None,
      Set(classOf[java.lang.Boolean], classOf[Boolean]),
      classOf[AssertTrue]),
    "validation.countryCode" -> AnnotationMetaData(
      None,
      Set(classOf[String]),
      classOf[CountryCode]),
    "validation.EAN" -> AnnotationMetaData(None, Set(classOf[String]), classOf[EAN]),
    "validation.email" -> AnnotationMetaData(
      None,
      Set(classOf[String]),
      classOf[jakarta.validation.constraints.Email]),
    "validation.ISBN" -> AnnotationMetaData(None, Set(classOf[String]), classOf[ISBN]),
    "validation.length.min" -> AnnotationMetaData(
      // we only need Java type here because the underlying Hibernator validator
      // defines the annotation value in Java.
      Some(classOf[java.lang.Integer]),
      Set(classOf[String]),
      classOf[Length]
    ),
    "validation.length.max" -> AnnotationMetaData(
      // we only need Java type here because the underlying Hibernator validator
      // defines the annotation value in Java.
      Some(classOf[java.lang.Integer]),
      Set(classOf[String]),
      classOf[Length]
    ),
    // "validation.max" and "validation.min" can be applied to numeric fields,
    // their annotation value is required to be Long. This matches the Jakarta
    // validation specification for Max and Min.
    "validation.max" -> AnnotationMetaData(
      // we only need Java type here because the underlying Hibernator validator
      // defines the annotation value in Java.
      Some(classOf[java.lang.Long]),
      NumericClasses,
      classOf[Max]
    ),
    "validation.min" -> AnnotationMetaData(
      // we only need Java type here because the underlying Hibernator validator
      // defines the annotation value in Java.
      Some(classOf[java.lang.Long]),
      NumericClasses,
      classOf[Min]
    ),
    // "validation.negative", "validation.negativeOrZero", "validation.positive",
    // "validation.positiveOrZero" can be applied to numeric values.
    "validation.negative" -> AnnotationMetaData(None, NumericClasses, classOf[Negative]),
    "validation.negativeOrZero" -> AnnotationMetaData(
      None,
      NumericClasses,
      classOf[NegativeOrZero]),
    // "validation.notEmpty" can be applied to String and collections.
    "validation.notEmpty" -> AnnotationMetaData(
      None,
      BaseCollectionClasses + classOf[String],
      classOf[jakarta.validation.constraints.NotEmpty]),
    "validation.positive" -> AnnotationMetaData(None, NumericClasses, classOf[Positive]),
    "validation.positiveOrZero" -> AnnotationMetaData(
      None,
      NumericClasses,
      classOf[PositiveOrZero]),
    // "validation.size" can be applied to collections.
    "validation.size.min" -> AnnotationMetaData(
      // we only need Java type here because the underlying Hibernator validator
      // defines the annotation value in Java.
      Some(classOf[java.lang.Integer]),
      BaseCollectionClasses,
      classOf[Size]
    ),
    "validation.size.max" -> AnnotationMetaData(
      // we only need Java type here because the underlying Hibernator validator
      // defines the annotation value in Java.
      Some(classOf[java.lang.Integer]),
      BaseCollectionClasses,
      classOf[Size]
    ),
    "validation.UUID" -> AnnotationMetaData(None, Set(classOf[String]), classOf[UUID])
  )

  private[twitter] val keys = metadata.keySet
}
