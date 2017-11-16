package com.twitter.scrooge

import com.twitter.io.Buf
import com.twitter.scrooge.HeaderMap._

object Request {

  def apply[Args <: ThriftStruct](args: Args): Request[Args] = {
    new Request(args)
  }

  def apply[Args <: ThriftStruct](headers: Map[String, Seq[Buf]], args: Args): Request[Args] = {
    new Request(headers, args)
  }

  def apply[Args <: ThriftStruct](bufs: Seq[(Buf, Buf)], args: Args): Request[Args] = {
    val headers = bufs.groupBy(keyGroupByFn).map { case (key, headersBufs) =>
      (key, headersBufs.map { case (_, headerValueBuf) => headerValueBuf })
    }
    new Request(headers, args)
  }
}

class Request[+Args <: ThriftStruct] private (headerMap: Map[String, Seq[Buf]], val args: Args) {
  val headers: HeaderMap = HeaderMap(headerMap)

  private def this(args: Args) = this(Map.empty[String, Seq[Buf]], args)

  /**
   * Set a simple header value with String types.
   *
   * @param headerKey String header key
   * @param headerValue String header value (will be stored as `Seq[Buf]`).
   * @return a new `Request` with the given header added to the contained headers.
   */
  def setHeader(headerKey: String, headerValue: String): Request[Args] = {
    Request(
      headers = headers.toMap + (headerKey -> Seq(Buf.Utf8(headerValue))),
      args = args)
  }

  /**
   * Set a header with the given String key and `Buf` values.
   *
   * @param headerKey String header key
   * @param headerValues Buf header values.
   * @return a new `Request` with the given header added to the contained headers.
   */
  def setHeader(headerKey: String, headerValues: Buf*): Request[Args] = {
    Request(
      headers = headers.toMap + (headerKey -> headerValues),
      args = args)
  }
}
