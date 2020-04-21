package com.twitter.scrooge.internal

import com.twitter.scrooge.ThriftStruct
import org.apache.thrift.TApplicationException
import org.apache.thrift.protocol.TProtocolException

/**
 * Internal helpers for Scrooge classes - should not be called by user code
 */
object ApplicationExceptions {

  /**
   * Given a service name, return a TApplicationException of type MISSING_RESULT
   * where the message includes the service name
   * @param serviceName
   * @return
   */
  def missingResult(serviceName: String): TApplicationException = {
    new TApplicationException(
      TApplicationException.MISSING_RESULT,
      serviceName + " failed: unknown result"
    )
  }

  /**
   * Given a message and the expected and actual type (byte representation), throws a
   * TProtocolException - will call String.format on the message with the ttypeToString of the
   * expected and actual type bytes as the two args.
   * @param message
   * @param expectedType
   * @param actualType
   */
  @throws[TProtocolException]
  def throwWrongFieldTypeException(message: String, expectedType: Byte, actualType: Byte): Unit = {
    throw new TProtocolException(
      String.format(
        message,
        ThriftStruct.ttypeToString(expectedType),
        ThriftStruct.ttypeToString(actualType)))
  }

}
