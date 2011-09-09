{{functionDecl}} = {
  encodeRequest("{{name}}", {{localName}}_args({{argNames}})) flatMap { this.service } flatMap {
    decodeResponse(_, {{localName}}_result.decoder)
  } flatMap { result =>
    {{resultUnwrapper}}
  }
}