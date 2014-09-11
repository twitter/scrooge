private[this] object {{__stats_name}} {
  val RequestsCounter = scopedStats.scope("{{clientFuncNameForWire}}").counter("requests")
  val SuccessCounter = scopedStats.scope("{{clientFuncNameForWire}}").counter("success")
  val FailuresCounter = scopedStats.scope("{{clientFuncNameForWire}}").counter("failures")
  val FailuresScope = scopedStats.scope("{{clientFuncNameForWire}}").scope("failures")
}
{{#headerInfo}}{{>header}}{{/headerInfo}} = {
  {{__stats_name}}.RequestsCounter.incr()
  this.service(encodeRequest("{{clientFuncNameForWire}}", {{ArgsStruct}}({{argNames}}))) flatMap { response =>
    val result = decodeResponse(response, {{ResultStruct}})
    val exception: Future[Nothing] =
{{#hasThrows}}
      if (false)
        null // can never happen, but needed to open a block
{{#throws}}
      else if (result.{{throwName}}.isDefined)
        Future.exception(setServiceName(result.{{throwName}}.get))
{{/throws}}
      else
        null
{{/hasThrows}}
{{^hasThrows}}
      null
{{/hasThrows}}

{{#isVoid}}
    if (exception != null) exception else Future.Done
{{/isVoid}}
{{^isVoid}}
    if (result.success.isDefined)
      Future.value(result.success.get)
    else if (exception != null)
      exception
    else
      Future.exception(missingResult("{{clientFuncNameForWire}}"))
{{/isVoid}}
  } respond {
    case Return(_) =>
      {{__stats_name}}.SuccessCounter.incr()
    case Throw(ex) =>
      {{__stats_name}}.FailuresCounter.incr()
      {{__stats_name}}.FailuresScope.counter(ex.getClass.getName).incr()
  }
}
