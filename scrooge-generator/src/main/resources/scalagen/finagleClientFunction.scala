private[this] object {{__stats_name}} {
  val RequestsCounter = scopedStats.scope("{{clientFuncNameForWire}}").counter("requests")
  val SuccessCounter = scopedStats.scope("{{clientFuncNameForWire}}").counter("success")
  val FailuresCounter = scopedStats.scope("{{clientFuncNameForWire}}").counter("failures")
  val FailuresScope = scopedStats.scope("{{clientFuncNameForWire}}").scope("failures")
}
{{#functionInfo}}
val {{clientFuncNameForWire}}{{ServiceName}}ReplyDeserializer: Array[Byte] => _root_.com.twitter.util.Try[{{typeName}}] = {
  response: Array[Byte] => {
    val result = decodeResponse(response, {{funcObjectName}}.Result)

    result.firstException() match {
      case Some(exception) => _root_.com.twitter.util.Throw(setServiceName(exception))
      case _ => result.successField match {
        case Some(success) => _root_.com.twitter.util.Return(success)
        case _ => _root_.com.twitter.util.Throw(missingResult("{{clientFuncNameForWire}}"))
      }
    }
  }
}
{{>function}} = {
  {{__stats_name}}.RequestsCounter.incr()
  val inputArgs = {{funcObjectName}}.Args({{argNames}})

  val serdeCtx = new _root_.com.twitter.finagle.thrift.DeserializeCtx[{{typeName}}](inputArgs, {{clientFuncNameForWire}}{{ServiceName}}ReplyDeserializer)
  _root_.com.twitter.finagle.context.Contexts.local.let(
    _root_.com.twitter.finagle.thrift.DeserializeCtx.Key,
    serdeCtx,
    _root_.com.twitter.finagle.thrift.Headers.Request.Key,
    _root_.com.twitter.finagle.thrift.Headers.Request.empty
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
