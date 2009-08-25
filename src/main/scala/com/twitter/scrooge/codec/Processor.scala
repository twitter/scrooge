package com.twitter.scrooge.codec

import scala.reflect.Manifest
import net.lag.naggati.{End, ProtocolError, Step}

abstract class Processor extends ((Buffer => Step) => Step) {
  def apply(f: Buffer => Step): Step

  def noExceptions[A] = new PartialFunction[A, Unit] {
    def isDefinedAt(x: A) = false
    def apply(x: A) = ()
  }

  def handleMethod[RV, A <: ThriftSerializable[A], R <: ThriftResult[R, RV]](f: Buffer => Step,
                   request: RequestHeader)(call: A => RV)(exceptionHandler: PartialFunction[(R, Exception), Unit])(
                   implicit argsManifest: Manifest[A], resultManifest: Manifest[R]): Step = {
    val args = argsManifest.create()
    val result = resultManifest.create()
    args.decode { args =>
      result.clearIsSet()
      try {
        val rv = call(args)
        result._rv__isSet = true
        result._rv = rv
      } catch {
        case e: Exception =>
          if (exceptionHandler.isDefinedAt((result, e))) {
            exceptionHandler((result, e))
          } else {
            throw e
          }
      }
      val buffer = new Buffer()
      buffer.writeRequestHeader(RequestHeader(MessageType.REPLY, request.methodName, request.sequenceId))
      result.encode(buffer)
      f(buffer)
    }
  }


  implicit def manifest2creator[T](m: Manifest[T]): Creator[T] = new Creator(m)

  class Creator[T](m: Manifest[T]) {
    def create(): T = m.erasure.newInstance().asInstanceOf[T]
  }
}
