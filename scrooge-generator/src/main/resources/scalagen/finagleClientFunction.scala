private[this] object {{__stats_name}} {
  val RequestsCounter = scopedStats.scope("{{clientFuncName}}").counter("requests")
  val SuccessCounter = scopedStats.scope("{{clientFuncName}}").counter("success")
  val FailuresCounter = scopedStats.scope("{{clientFuncName}}").counter("failures")
  val FailuresScope = scopedStats.scope("{{clientFuncName}}").scope("failures")
}

{{#headerInfo}}{{>header}}{{/headerInfo}} = {
  {{__stats_name}}.RequestsCounter.incr()
  this.service(encodeRequest("{{clientFuncName}}", {{ArgsStruct}}({{argNames}}))) flatMap { response =>
    val result = decodeResponse(response, {{ResultStruct}})
    val exception =
{{#hasThrows}}
      ({{#throws}}result.{{throwName}}{{/throws| orElse }}).map(Future.exception)
{{/hasThrows}}
{{^hasThrows}}
      None
{{/hasThrows}}
{{#void}}
    Future.Done
{{/void}}
{{^void}}
    exception.orElse(result.success.map(Future.value)).getOrElse(Future.exception(missingResult("{{clientFuncName}}")))
{{/void}}
  } rescue {
    case ex: SourcedException => {
      if (this.serviceName != "") { ex.serviceName = this.serviceName }
      Future.exception(ex)
    }
  } onSuccess { _ =>
    {{__stats_name}}.SuccessCounter.incr()
  } onFailure { ex =>
    {{__stats_name}}.FailuresCounter.incr()
    {{__stats_name}}.FailuresScope.counter(ex.getClass.getName).incr()
  }
}
