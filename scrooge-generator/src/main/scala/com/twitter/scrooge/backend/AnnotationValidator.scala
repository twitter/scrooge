package com.twitter.scrooge.backend

import com.twitter.scrooge.thrift_validation.DefaultAnnotations
import com.twitter.scrooge.thrift_validation.DefaultAnnotations.AnnotationMetaData
import java.util.logging.Logger
import scala.collection.mutable

/**
 * A singleton helper object to validate annotations applied to
thrift IDLs. The validation includes:
 *   1. The given annotation can be applied to the field Class
 *   2. The given annotation value matches the annotation type
 *   3. The given annotation is defined in `UtilValidator`
 *
 * If any of the above criteria fails, an
 * [[IllegalArgumentException]] will be thrown during code
 * generation for internal services.
 *
 * @note Only validation for built-in constraint annotations
 *       is supported for now
 */
private[backend] object AnnotationValidator {
  private val logger: Logger = Logger.getLogger(getClass.getName)

  private val defaultAnnotationKeys = DefaultAnnotations.metadata.keySet

  def isDefaultAnnotation(annotationKey: String): Boolean =
    defaultAnnotationKeys.contains(annotationKey)

  /**
   *  Validate:
   *   1. The given annotation can be applied to the field Class
   *   2. The given annotation value matches the annotation type
   *   3. The given annotation is defined in either the default
   *   `UtilValidator`
   *
   * @param fieldClazz a Set of [[Class]] that the annotation is
   *                   allowed to be applied.
   * @param fieldAnnotations a key value Map for annotations to
   *                         be validated.
   * @return a Set with error messages of all invalid annotations.
   */
  def validateAnnotations(
    fieldClazz: Set[Class[_]],
    fieldAnnotations: Map[String, String]
  ): Iterable[String] =
    fieldAnnotations.flatMap {
      case (key, _) =>
        validateAnnotation(key, fieldAnnotations(key), fieldClazz)
    }

  /**
   * Validate:
   *   1. The given annotation can be applied to the field Class
   *   2. The given annotation value matches the annotation type
   *   3. The given annotation is defined in either the default
   *   `UtilValidator`
   */
  private def validateAnnotation(
    annotationKey: String,
    annotationValue: String,
    fieldClazz: Set[Class[_]]
  ): Set[String] =
    if (isDefaultAnnotation(annotationKey)) {
      validateDefaultAnnotation(annotationKey, annotationValue, fieldClazz)
    } else {
      if (annotationKey.startsWith("validation.")) {
        logger.warning(
          s"Annotation validation key: $annotationKey is not supported " +
            s"and it was not validated. Only the built-in scrooge annotations " +
            s"are supported for validation. The annotation value was $annotationValue.")
      }
      // skip validations for all other annotations
      Set.empty
    }

  /**
   * Validate:
   *   1. The default annotation can be applied to the field Class
   *   2. The default annotation value matches the annotation type
   */
  private def validateDefaultAnnotation(
    annotationKey: String,
    annotationValue: String,
    fieldClazz: Set[Class[_]]
  ): Set[String] =
    DefaultAnnotations.metadata.get(annotationKey) match {
      case Some(AnnotationMetaData(annotationClazz, allowedClazz, _)) =>
        val result = mutable.Set.empty[String]
        if (annotationClazz.nonEmpty) {
          val clazz = annotationClazz.get
          // validate if the given annotation value matches the annotation type
          try {
            // the annotation class is defined with the Java class as a private
            // field in `DefaultAnnotations.AnnotationMetaData`, so we only need
            // to compare with Java class.
            // we only need Java type here because the underlying Hibernator
            // validator defines the annotation value in Java.
            // we only need to check for `Integer` and `Long` since they are the
            // only allowed annotation classes for default annotations.
            if (clazz == classOf[java.lang.Integer]) annotationValue.toInt
            if (clazz == classOf[java.lang.Long]) annotationValue.toLong
          } catch {
            case _: java.lang.NumberFormatException =>
              val errorMessage =
                s"The annotation $annotationKey requires a value of type ${clazz.getSimpleName}, " +
                  s"the annotation value ${truncate(annotationValue)} " +
                  s"is not of type ${clazz.getSimpleName}"
              result += errorMessage
          }
        }
        // validate the given annotation can be applied to the given field type
        if (fieldClazz.nonEmpty && allowedClazz.intersect(fieldClazz).isEmpty) {
          val errorMessage =
            s"The annotation $annotationKey can not be applied to the field with type ${fieldClazz
              .map(_.getSimpleName)
              .mkString(", ")}, the allowed Classes are: ${allowedClazz.map(_.getSimpleName).mkString(", ")}"
          result += errorMessage
        }
        result.toSet
      case _ => Set.empty[String]
    }

  private def truncate(annotationValue: String): String =
    // we truncate the String in case the value is too long,
    // use 21 since the longest valid Long number is 20 characters
    if (annotationValue.length > 21) annotationValue.substring(0, 21) + "..."
    else annotationValue
}
