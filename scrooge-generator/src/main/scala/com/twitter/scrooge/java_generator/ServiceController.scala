package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast._
import com.twitter.scrooge.ast.Service
import scala.Some

class ServiceController(service: Service, generator: ApacheJavaGenerator, ns: Option[Identifier])
  extends BaseController(generator, ns) {
  val name = service.sid.name
  val extends_iface = service.parent match {
    case Some(parent) => Map("name" -> parent.sid.name)
    case None => false
  }
  val functions = service.functions map { f => new FunctionController(f, generator, ns) }
}
