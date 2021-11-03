package com.twitter.scrooge

import com.twitter.scrooge.ValidatingThriftStructCodec3.utilValidator
import com.twitter.scrooge.thrift_validation.ThriftValidationViolation
import com.twitter.scrooge.thrift_validation.ThriftValidator
import com.twitter.scrooge.validation.Issue

object ValidatingThriftStructCodec3 {
  final val utilValidator: UtilValidator = UtilValidator()
}

abstract class ValidatingThriftStructCodec3[T <: ThriftStruct] extends ThriftStructCodec3[T] {

  /**
   * Checks that the struct is a valid as a new instance. If there are any missing required or
   * construction required fields, return a non-empty Seq of Issues.
   */
  def validateNewInstance(item: T): Seq[Issue]

  /**
   * Validate that all validation annotations on the struct meet the criteria defined in the
   * corresponding [[com.twitter.scrooge.thrift_validation.ThriftConstraintValidator]].
   *
   * @param item the struct instance to validate.
   *
   * @return a set of [[ThriftValidationViolation]]. Return an empty set if all validations
   *         passed.
   */
  def validateInstanceValue(item: T): Set[ThriftValidationViolation]

  /**
   * Method that should be called on every field of a struct to validate new instances of that
   * struct. This should only called by the generated implementations of validateNewInstance.
   */
  final protected def validateField[U <: ValidatingThriftStruct[U]](any: Any): Seq[Issue] = {
    any match {
      // U is unchecked since it is eliminated by erasure, but we know that validatingStruct extends
      // from ValidatingThriftStruct. The code below should be safe for any ValidatingThriftStruct
      case validatingStruct: ValidatingThriftStruct[_] =>
        val struct: U = validatingStruct.asInstanceOf[U]
        struct._codec.validateNewInstance(struct)
      case map: collection.Map[_, _] =>
        map.flatMap {
          case (key, value) =>
            Seq(
              validateField(key),
              validateField(value)
            ).flatten
        }.toList
      case iterable: Iterable[_] => iterable.toList.flatMap(validateField)
      case option: Option[_] => option.toList.flatMap(validateField)
      case _ => Nil
    }
  }

  /**
   * Validate the annotations on each struct field. This should only be called by the generated
   * implementation of validateInstanceValue.
   *
   * @param fieldName name of the struct field.
   * @param fieldValue runtime value of the struct field.
   * @param fieldAnnotations annotations for each field on the IDL.
   * @param thriftValidatorOpt an Option of [[ThriftValidator]] instance. When present, custom
   *                           validations are defined.
   * @tparam U type of the field.
   * @return a set of [[ThriftValidationViolation]]. Return an empty set if all validations
   *         passed.
   */
  final protected def validateFieldValue[U <: ValidatingThriftStruct[U]](
    fieldName: String,
    fieldValue: Any,
    fieldAnnotations: Map[String, String],
    thriftValidatorOpt: Option[ThriftValidator]
  ): Set[ThriftValidationViolation] =
    fieldValue match {
      // U is unchecked since it is eliminated by erasure, but we know that validatingStruct extends
      // from ValidatingThriftStruct. The code below should be safe for any ValidatingThriftStruct
      case validatingStruct: ValidatingThriftStruct[_] =>
        val struct: U = validatingStruct.asInstanceOf[U]
        // recursively validate for nested struct
        struct._codec.validateInstanceValue(struct) ++
          withValidator(fieldName, fieldValue, fieldAnnotations, thriftValidatorOpt)
      case _ =>
        // if the field is not a struct, invoke thriftValidator for field validations
        withValidator(fieldName, fieldValue, fieldAnnotations, thriftValidatorOpt)
    }

  private def withValidator(
    fieldName: String,
    fieldValue: Any,
    fieldAnnotations: Map[String, String],
    thriftValidatorOpt: Option[ThriftValidator]
  ): Set[ThriftValidationViolation] =
    thriftValidatorOpt match {
      case Some(thriftValidator) =>
        // perform both default and custom validations
        utilValidator.validateField(fieldName, fieldValue, fieldAnnotations) ++
          thriftValidator.validateField(fieldName, fieldValue, fieldAnnotations)
      case _ => utilValidator.validateField(fieldName, fieldValue, fieldAnnotations)
    }
}
