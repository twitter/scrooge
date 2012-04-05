functionMap.update("{{name}}", new Function2<TProtocol, Int, Future<{{typeName}}>>() {
  public Future<{{typeName}}> apply(TProtocol iprot, Int seqid) {
    try {
      Args args = {{localName}}_args.decode(iprot);
      iprot.readMessageEnd();
      Future<{{typeName}}> result;
      try {
        result = iface.{{localName}}({{argNames}});
        return result.flatMap(new Function<{{typeName}}, Future<{{typeName}}>>() {
          public Future<{{typeName}}> apply({{typeName}} value){
            return reply("{{name}}", seqid, {{localName}}_result({{resultNamedArg}}));
          }
        });
{{#exceptions}}
      } catch ({{exceptionType}} e) {
        return reply("{{name}}", seqid, {{localName}}_result({{fieldName}} = new Some(e)));
{{/exceptions}}
      } catch (Throwable t) {
        return Future.exception(e);
      }
    } catch (TProtocolException e) {
      iprot.readMessageEnd();
      return exception("{{name}}", seqid, TApplicationException.PROTOCOL_ERROR, e.getMessage);
    } catch (Throwable t) {
      return Future.exception(t);
    }
  }
});