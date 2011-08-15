{{functionDecl}} = {
  encodeRequest("{{name}}", {{name}}_args({{argNames}})) flatMap { this.service } flatMap {
    decodeResponse(_, {{name}}_result.decoder)
  } flatMap { result =>
    {{resultUnwrapper}}
  }
}