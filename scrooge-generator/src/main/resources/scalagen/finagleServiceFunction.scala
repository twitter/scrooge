addFunction("{{serviceFuncNameForWire}}", { (iprot: TProtocol, seqid: Int) =>
  try {
    val args = {{ArgsStruct}}.decode(iprot)
    iprot.readMessageEnd()
    (try {
      iface.{{serviceFuncNameForCompile}}({{argNames}})
    } catch {
      case e: Exception => Future.exception(e)
    }) flatMap { value: {{typeName}} =>
      reply("{{serviceFuncNameForWire}}", seqid, {{ResultStruct}}({{resultNamedArg}}))
    } rescue {
{{#exceptions}}
      case e: {{exceptionType}} => {
        reply("{{serviceFuncNameForWire}}", seqid, {{ResultStruct}}({{fieldName}} = Some(e)))
      }
{{/exceptions}}
      case e => Future.exception(e)
    }
  } catch {
    case e: TProtocolException => {
      iprot.readMessageEnd()
      exception("{{serviceFuncNameForWire}}", seqid, TApplicationException.PROTOCOL_ERROR, e.getMessage)
    }
    case e: Exception => Future.exception(e)
  }
})
