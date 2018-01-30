package com.twitter.scrooge

import com.twitter.io.Buf
import org.apache.thrift.TApplicationException

/**
 * A [[RichResponse]] tells filters richer information of a response, including the corresponding
 * request, intermediate forms of the response and final response in byte buffer.
 *
 * @tparam In type of the request
 * @tparam Out intermediate form of the response
 */
sealed trait RichResponse[In, Out] {
  def input: In
  def response: Buf
}

/**
 * A [[ProtocolExceptionResponse]] represents a response which throws a thrift application
 * exception of protocol error.
 * @note while this is a public API, it is only intended for use by the generated code.
 *
 * @param exception a TApplicationException of protocol error
 */
case class ProtocolExceptionResponse[In, Out] (
  input: In,
  response: Buf,
  exception: TApplicationException
) extends RichResponse[In, Out]

/**
 * A [[SuccessfulResponse]] represents a successful response.
 * @note while this is a public API, it is only intended for use by the generated code.
 *
 * @param result a result contains deserialized successful response
 */
case class SuccessfulResponse[In, Out] (
  input: In,
  response: Buf,
  result: Out
) extends RichResponse[In, Out]

/**
 * A [[ThriftExceptionResponse]] represents a response which throws thrift application exceptions
 * defined in IDL.
 * @note while this is a public API, it is only intended for use by the generated code.
 *
 * @param ex the thrift application exception
 */
case class ThriftExceptionResponse[In, Out] (
  input: In,
  response: Buf,
  ex: ThriftException
) extends RichResponse[In, Out]
