functionMap("{{name}}") = { (iprot: TProtocol, seqid: Int) =>
  try {
    val args = {{name}}_args.decoder(iprot)
    iprot.readMessageEnd()
    (try {
      iface.{{name}}({{argNames}})
    } catch {
      case e: Exception => Future.exception(e)
    }) flatMap { value: {{scalaType}} =>
      reply("{{name}}", seqid, {{name}}_result({{resultNamedArg}}))
    } rescue {
      {{#exceptions}}
      case e: {{exceptionType}} =>
        reply("{{name}}", seqid, {{name}}_result({{fieldName}} = Some(e)))
      {{/exception}}
      case e: Throwable =>
        exception("{{name}}", seqid, TApplicationException.INTERNAL_ERROR, "Internal error processing {{name}}")
    }
  } catch {
    case e: TProtocolException =>
      iprot.readMessageEnd()
      exception("{{name}}", seqid, TApplicationException.PROTOCOL_ERROR, e.getMessage)
    case e: Exception =>
      Future.exception(e)
  }
}