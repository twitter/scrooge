private[this] object __stats_{{name}} {
  val RequestsCounter = scopedStats.scope("{{name}}").counter("requests")
  val SuccessCounter = scopedStats.scope("{{name}}").counter("success")
  val FailuresCounter = scopedStats.scope("{{name}}").counter("failures")
  val FailuresScope = scopedStats.scope("{{name}}").scope("failures")
}

{{#headerInfo}}{{>header}}{{/headerInfo}} = {
  __stats_{{name}}.RequestsCounter.incr()
  this.service(encodeRequest("{{name}}", {{localName}}_args({{argNames}}))) flatMap { response =>
    val result = decodeResponse(response, {{localName}}_result)
    val exception =
{{#hasThrows}}
      ({{#throws}}result.{{name}}{{/throws| orElse }}).map(Future.exception)
{{/hasThrows}}
{{^hasThrows}}
      None
{{/hasThrows}}
{{#void}}
    Future.Done
{{/void}}
{{^void}}
    exception.orElse(result.success.map(Future.value)).getOrElse(Future.exception(missingResult("{{name}}")))
{{/void}}
  } rescue {
    case ex: SourcedException => {
      if (this.serviceName != "") { ex.serviceName = this.serviceName }
      Future.exception(ex)
    }
  } onSuccess { _ =>
    __stats_{{name}}.SuccessCounter.incr()
  } onFailure { ex =>
    __stats_{{name}}.FailuresCounter.incr()
    __stats_{{name}}.FailuresScope.counter(ex.getClass.getName).incr()
  }
}
