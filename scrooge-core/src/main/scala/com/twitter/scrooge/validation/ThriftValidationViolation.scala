package com.twitter.scrooge.validation

/**
 * To store the data for failed validations.
 * @param fieldName the name of the field being validated.
 * @param fieldValue the value of the field being validated.
 * @param violationMessage the validation failure message.
 */
case class ThriftValidationViolation(
  fieldName: String,
  fieldValue: Any,
  violationMessage: String)
