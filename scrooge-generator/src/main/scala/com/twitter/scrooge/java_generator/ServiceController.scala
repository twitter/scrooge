package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast._
import com.twitter.scrooge.ast.Service
import com.twitter.scrooge.backend.ServiceOption
import com.twitter.scrooge.backend.WithJavaPassThrough

class ServiceController(
  service: Service,
  serviceOptions: Set[ServiceOption],
  generator: ApacheJavaGenerator,
  ns: Option[Identifier])
    extends TypeController(service, generator, ns) {
  val extends_iface: Any = service.parent match {
    case Some(parent) =>
      Map(
        "parent_name" ->
          generator.qualifyNamedType(parent.sid, parent.filename).fullName
      )
    case None =>
      false
  }

  val is_passthrough_service: Boolean = serviceOptions.contains(WithJavaPassThrough)

  val functions: Seq[FunctionController] = service.functions map { f =>
    new FunctionController(f, serviceOptions, generator, ns, service.sid.fullName)
  }

  // if any args in any functions defined in the service has
  // annotations that start with `validation.`, return true
  val hasValidationAnnotation: Boolean =
    service.functions.flatMap(_.args).exists(FieldController.hasValidationAnnotation)
}
