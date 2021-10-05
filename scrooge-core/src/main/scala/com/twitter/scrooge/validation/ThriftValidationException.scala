package com.twitter.scrooge.validation

/**
 *  An exception which is used to communicate when a thriftValidation
 *  has failed with the respective [[ThriftValidationViolation]]
 *  @param endpoint   thrift method the invalid request tries to reach
 *  @param requestClazz  the type of request that was passed in
 *  @param validationViolations all violations collected while deserializing the thrift object
 */

final case class ThriftValidationException(
  endpoint: String,
  requestClazz: Class[_],
  validationViolations: Set[ThriftValidationViolation])
    extends Exception {

  override def getMessage: String =
    s" The validation for request ${requestClazz.getName} to endpoint $endpoint failed with messages: ${validationViolations
      .mkString(",")}"

}
