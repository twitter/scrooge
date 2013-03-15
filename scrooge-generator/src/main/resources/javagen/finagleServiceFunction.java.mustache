addFunction("{{serviceFuncNameForWire}}", new Function2<TProtocol, Integer, Future<byte[]>>() {
  public Future<byte[]> apply(TProtocol iprot, final Integer seqid) {
    try {
      {{ArgsStruct}} args = {{ArgsStruct}}.decode(iprot);
      iprot.readMessageEnd();
      Future<{{typeName}}> result;
      try {
        result = iface.{{serviceFuncNameForCompile}}({{argNames}});
      } catch (Throwable t) {
        result = Future.exception(t);
      }
      return result.flatMap(new Function<{{typeName}}, Future<byte[]>>() {
        public Future<byte[]> apply({{typeName}} value){
          return reply("{{serviceFuncNameForWire}}", seqid, new {{ResultStruct}}.Builder(){{^isVoid}}.success(value){{/isVoid}}.build());
        }
      }).rescue(new Function<Throwable, Future<byte[]>>() {
        public Future<byte[]> apply(Throwable t) {
{{#exceptions}}
          if (t instanceof {{exceptionType}}) {
            return reply("{{ServiceName}}", seqid, new {{ResultStruct}}.Builder().{{fieldName}}(({{exceptionType}}) t).build());
          }
{{/exceptions}}
          return Future.exception(t);
        }
      });
    } catch (TProtocolException e) {
      try {
        iprot.readMessageEnd();
        return exception("{{serviceFuncNameForWire}}", seqid, TApplicationException.PROTOCOL_ERROR, e.getMessage());
      } catch (Exception unrecoverable) {
        return Future.exception(unrecoverable);
      }
    } catch (Throwable t) {
      return Future.exception(t);
    }
  }
});
