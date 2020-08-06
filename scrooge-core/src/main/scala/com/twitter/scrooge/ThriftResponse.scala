package com.twitter.scrooge

trait ThriftResponse[Result] {
  def successField: Option[Result]
  def exceptionFields: Iterable[Option[ThriftException]]

  /**
   * Return the first nonempty exception field.
   */
  def firstException(): Option[ThriftException] =
    exceptionFields.collectFirst(ThriftResponse.exceptionIsDefined)
}

object ThriftResponse {
  private val exceptionIsDefined: PartialFunction[Option[ThriftException], ThriftException] = {
    case Some(exception) => exception
  }
}
