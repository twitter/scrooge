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
 * A [[ProtocolException]] represents a response which throws a thrift application
 * exception of protocol error.
 *
 * @param exception a TApplicationException of protocol error
 */
case class ProtocolException[In, Out] (
  input: In,
  response: Buf,
  exception: TApplicationException
) extends RichResponse[In, Out]

/**
 * A [[SuccessfulResult]] represents a successful response.
 *
 * @param result a result contains deserialized successful response
 */
case class SuccessfulResult[In, Out] (
  input: In,
  response: Buf,
  result: Out
) extends RichResponse[In, Out]

/**
 * A [[ThriftExceptionResult]] represents a response which throws thrift application exceptions
 * defined in IDL.
 *
 * @param result a result contains thrift application exceptions
 */
case class ThriftExceptionResult[In, Out] (
  input: In,
  response: Buf,
  result: Out
) extends RichResponse[In, Out]
