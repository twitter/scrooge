functionMap("{{name}}") = { (iprot: TProtocol, seqid: Int) =>
  try {
    val args = {{localName}}_args.decoder(iprot)
    iprot.readMessageEnd()
    (try {
      iface.{{localName}}({{argNames}})
    } catch {
      case e: Exception => Future.exception(e)
    }) flatMap { value: {{typeName}} =>
      reply("{{name}}", seqid, {{localName}}_result({{resultNamedArg}}))
    } rescue {
{{#exceptions}}
      case e: {{exceptionType}} => {
        reply("{{name}}", seqid, {{localName}}_result({{fieldName}} = Some(e)))
      }
{{/exceptions}}
      case e => Future.exception(e)
    }
  } catch {
    case e: TProtocolException => {
      iprot.readMessageEnd()
      exception("{{name}}", seqid, TApplicationException.PROTOCOL_ERROR, e.getMessage)
    }
    case e: Exception => Future.exception(e)
  }
}
