private[this] object {{__stats_name}} {
  val RequestsCounter = scopedStats.scope("{{clientFuncNameForWire}}").counter("requests")
  val SuccessCounter = scopedStats.scope("{{clientFuncNameForWire}}").counter("success")
  val FailuresCounter = scopedStats.scope("{{clientFuncNameForWire}}").counter("failures")
  val FailuresScope = scopedStats.scope("{{clientFuncNameForWire}}").scope("failures")
}
{{#functionInfo}}
{{>function}} = {
  {{__stats_name}}.RequestsCounter.incr()
  val inputArgs = {{funcObjectName}}.Args({{argNames}})
  val replyDeserializer: Array[Byte] => _root_.com.twitter.util.Try[{{typeName}}] =
    response => {
      val result = decodeResponse(response, {{funcObjectName}}.Result)
      val exception: Throwable =
{{#hasThrows}}
      if (false)
        null // can never happen, but needed to open a block
{{#throws}}
      else if (result.{{throwName}}.isDefined)
        setServiceName(result.{{throwName}}.get)
{{/throws}}
      else
        null
{{/hasThrows}}
{{^hasThrows}}
      null
{{/hasThrows}}

{{#isVoid}}
      if (exception != null) _root_.com.twitter.util.Throw(exception) else Return.Unit
{{/isVoid}}
{{^isVoid}}
      if (result.success.isDefined)
        _root_.com.twitter.util.Return(result.success.get)
      else if (exception != null)
        _root_.com.twitter.util.Throw(exception)
      else
        _root_.com.twitter.util.Throw(missingResult("{{clientFuncNameForWire}}"))
{{/isVoid}}
    }

  val serdeCtx = new _root_.com.twitter.finagle.thrift.DeserializeCtx[{{typeName}}](inputArgs, replyDeserializer)
  _root_.com.twitter.finagle.context.Contexts.local.let(
    _root_.com.twitter.finagle.thrift.DeserializeCtx.Key,
    serdeCtx
  ) {
    val serialized = encodeRequest("{{clientFuncNameForWire}}", inputArgs)
    this.service(serialized).flatMap { response =>
      Future.const(serdeCtx.deserialize(response))
    }.respond { response =>
      val responseClass = responseClassifier.applyOrElse(
        ctfs.ReqRep(inputArgs, response),
        ctfs.ResponseClassifier.Default)
      responseClass match {
        case ctfs.ResponseClass.Successful(_) =>
          {{__stats_name}}.SuccessCounter.incr()
        case ctfs.ResponseClass.Failed(_) =>
          {{__stats_name}}.FailuresCounter.incr()
          response match {
            case Throw(ex) =>
              setServiceName(ex)
              {{__stats_name}}.FailuresScope.counter(Throwables.mkString(ex): _*).incr()
            case _ =>
          }
      }
    }
  }
}
{{/functionInfo}}
