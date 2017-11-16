package com.twitter.scrooge

import com.twitter.io.Buf
import com.twitter.scrooge.HeaderMap._

object Response {

  def apply[SuccessType](value: SuccessType): Response[SuccessType] = {
    new Response(value)
  }

  def apply[SuccessType](headers: Map[String, Seq[Buf]], value: SuccessType): Response[SuccessType] = {
    new Response(headers, value)
  }

  def apply[SuccessType](bufs: Seq[(Buf, Buf)], value: SuccessType): Response[SuccessType] = {
    val headers = bufs.groupBy(keyGroupByFn).map { case (key, headersBufs) =>
      (key, headersBufs.map { case (_, headerValueBuf) => headerValueBuf })
    }
    new Response(headers, value)
  }
}

class Response[+SuccessType] private (headerMap: Map[String, Seq[Buf]], val value: SuccessType) {
  val headers: HeaderMap = HeaderMap(headerMap)

  private def this(value: SuccessType) = this(Map.empty[String, Seq[Buf]], value)

  /**
   * Set a simple header value with String types.
   *
   * @param headerKey String header key
   * @param headerValue String header value (will be stored as `Seq[Buf]`).
   * @return a new `Response` with the given header added to the contained headers.
   */
  def setHeader(headerKey: String, headerValue: String): Response[SuccessType] = {
    Response(
      headers = headers.toMap + (headerKey -> Seq(Buf.Utf8(headerValue))),
      value = value)
  }

  /**
   * Set a header with the given String key and `Buf` values.
   *
   * @param headerKey String header key
   * @param headerValues Buf header values.
   * @return a new `Response` with the given header added to the contained headers.
   */
  def setHeader(headerKey: String, headerValues: Buf*): Response[SuccessType] = {
    Response(
      headers = headers.toMap + (headerKey -> headerValues),
      value = value)
  }
}
