{{functionDecl}} = {
  encodeRequest("{{name}}", {{localName}}_args({{argNames}})) flatMap { this.service } flatMap {
    decodeResponse(_, {{localName}}_result.decoder)
  } flatMap { result =>
    {{resultUnwrapper}}
  } rescue {
    case e: SourcedException =>
      serviceName foreach { e.serviceName = _ }
      Future.exception(e)
  }
}
