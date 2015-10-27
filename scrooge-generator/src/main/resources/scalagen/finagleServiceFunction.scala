private[this] object {{__stats_name}} {
  val RequestsCounter = scopedStats.scope("{{serviceFuncNameForWire}}").counter("requests")
  val SuccessCounter = scopedStats.scope("{{serviceFuncNameForWire}}").counter("success")
  val FailuresCounter = scopedStats.scope("{{serviceFuncNameForWire}}").counter("failures")
  val FailuresScope = scopedStats.scope("{{serviceFuncNameForWire}}").scope("failures")
}
addFunction("{{serviceFuncNameForWire}}", { (iprot: TProtocol, seqid: Int) =>
  try {
    {{__stats_name}}.RequestsCounter.incr()
    val args = {{funcObjectName}}.Args.decode(iprot)
    iprot.readMessageEnd()
    (try {
      iface.{{serviceFuncNameForCompile}}({{argNames}})
    } catch {
      case e: Exception => Future.exception(e)
    }).flatMap { value: {{typeName}} =>
      reply("{{serviceFuncNameForWire}}", seqid, {{funcObjectName}}.Result({{resultNamedArg}}))
    }.rescue {
{{#exceptions}}
      case e: {{exceptionType}} => {
        reply("{{serviceFuncNameForWire}}", seqid, {{funcObjectName}}.Result({{fieldName}} = Some(e)))
      }
{{/exceptions}}
      case e => Future.exception(e)
    }.respond {
      case Return(_) =>
        {{__stats_name}}.SuccessCounter.incr()
      case Throw(ex) =>
        {{__stats_name}}.FailuresCounter.incr()
        {{__stats_name}}.FailuresScope.counter(Throwables.mkString(ex): _*).incr()
    }
  } catch {
    case e: TProtocolException => {
      iprot.readMessageEnd()
      exception("{{serviceFuncNameForWire}}", seqid, TApplicationException.PROTOCOL_ERROR, e.getMessage)
    }
    case e: Exception => Future.exception(e)
  }
})
