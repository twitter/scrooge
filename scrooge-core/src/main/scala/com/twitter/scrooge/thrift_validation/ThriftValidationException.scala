package com.twitter.scrooge.thrift_validation
import scala.collection.JavaConverters._

object ThriftValidationException {

  /**
   *  A java compatible exception which is used to communicate when a thrift_validation
   *  has failed with the respective [[ThriftValidationViolation]]
   *
   *  @param endpoint   thrift method the invalid request tries to reach
   *  @param requestClazz  the type of request that was passed in
   *  @param validationViolations all violations collected while deserializing the thrift object
   */
  def create(
    endpoint: String,
    requestClazz: Class[_],
    validationViolations: java.util.Set[ThriftValidationViolation]
  ): ThriftValidationException =
    ThriftValidationException(endpoint, requestClazz, validationViolations.asScala.toSet)
}

/**
 *  An exception which is used to communicate when a thrift_validation
 *  has failed with the respective [[ThriftValidationViolation]]
 *  @param endpoint   thrift method the invalid request tries to reach
 *  @param requestClazz  the type of request that was passed in
 *  @param validationViolations all violations collected while deserializing the thrift object
 */

final case class ThriftValidationException(
  endpoint: String,
  requestClazz: Class[_],
  validationViolations: Set[ThriftValidationViolation])
    extends RuntimeException {

  override def getMessage: String =
    s" The validation for request ${requestClazz.getName} to endpoint $endpoint failed with messages: ${validationViolations
      .mkString(",")}"

}
