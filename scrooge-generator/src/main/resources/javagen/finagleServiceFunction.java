addFunction("{{name}}", new Function2<TProtocol, Integer, Future<byte[]>>() {
  public Future<byte[]> apply(TProtocol iprot, final Integer seqid) {
    try {
      {{localName}}_args args = {{localName}}_args.decode(iprot);
      iprot.readMessageEnd();
      Future<{{typeName}}> result;
      try {
        result = iface.{{localName}}({{argNames}});
      } catch (Throwable t) {
        result = Future.exception(t);
      }
      return result.flatMap(new Function<{{typeName}}, Future<byte[]>>() {
        public Future<byte[]> apply({{typeName}} value){
          return reply("{{name}}", seqid, new {{localName}}_result.Builder(){{^isVoid}}.success(value){{/isVoid}}.build());
        }
      }).rescue(new Function<Throwable, Future<byte[]>>() {
        public Future<byte[]> apply(Throwable t) {
{{#exceptions}}
          if (t instanceof {{exceptionType}}) {
            return reply("{{name}}", seqid, new {{localName}}_result.Builder().{{fieldName}}(({{exceptionType}}) t).build());
          }
{{/exceptions}}
          return Future.exception(t);
        }
      });
    } catch (TProtocolException e) {
      try {
        iprot.readMessageEnd();
        return exception("{{name}}", seqid, TApplicationException.PROTOCOL_ERROR, e.getMessage());
      } catch (Exception unrecoverable) {
        return Future.exception(unrecoverable);
      }
    } catch (Throwable t) {
      return Future.exception(t);
    }
  }
});
