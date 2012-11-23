addFunction("{{serviceFuncName}}", { (iprot: TProtocol, seqid: Int) =>
  try {
    val args = {{ArgsStruct}}.decode(iprot)
    iprot.readMessageEnd()
    (try {
      iface.{{serviceFuncName}}({{argNames}})
    } catch {
      case e: Exception => Future.exception(e)
    }) flatMap { value: {{typeName}} =>
      reply("{{serviceFuncName}}", seqid, {{ResultStruct}}({{resultNamedArg}}))
    } rescue {
{{#exceptions}}
      case e: {{exceptionType}} => {
        reply("{{serviceFuncName}}", seqid, {{ResultStruct}}({{fieldName}} = Some(e)))
      }
{{/exceptions}}
      case e => Future.exception(e)
    }
  } catch {
    case e: TProtocolException => {
      iprot.readMessageEnd()
      exception("{{serviceFuncName}}", seqid, TApplicationException.PROTOCOL_ERROR, e.getMessage)
    }
    case e: Exception => Future.exception(e)
  }
})
