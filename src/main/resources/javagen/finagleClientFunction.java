private StatsReceiver _{{name}}Scope = scopedStats.scope("{{name}}");
private Counter _{{name}}RequestsCounter = _{{name}}Scope.counter("requests");
private Counter _{{name}}SuccessCounter = _{{name}}Scope.counter("success");
private Counter _{{name}}FailuresCounter = _{{name}}Scope.counter("failures");
private Counter _{{name}}FailuresScope = _{{name}}Scope.scope("failures");

{{functionDecl}} {
  _{{name}}RequestsCounter.incr();
  encodeRequest("deliver", deliver_args(where)).flatMap(new Function<ThriftStruct, Future<Array<Byte>>>() {
    public Future<Array<Byte>> apply(ThriftStruct request) {
      return this.service.apply(request);
    }
  }).flatMap(new Function<Array<Byte>, {{type}}>() {
    public {{type}} apply(Array<Byte> in) {
      return decodeResponse(in, {{localName}}_result.decoder);
    }
  }).flatMap(new Function<Array<Byte>, {{type}}>() {
    public {{type}} apply(Array<Byte> in) {
      {{#hasThrows}}
      Exception exception = null;
      {{#throws}}if (exception == null && result.{{name}}.isDefined) exception = result.{{name}}.get;{{/throws}}
      if (exception != null) return Future.exception(exception);
      {{/hasThrows}}

      {{#void}}return Future.Done;{{/void}}
      {{^void}}
      if (result.success.isDefined()) return Future.value(result.success.get());
      return missingResult("{{name}}");
      {{/void}}
    }
  }).rescue(new Function<Throwable, Future<{{type}}>>() {
      public Future<{{type}}> apply(Throwable t) {
        // if (throwable instanceof SourcedException) ex.serviceName = this.serviceName;
        return Future.exception(ex);
      }
  }).onSuccess(new Function<{{type}}, Void>() {
    public Void apply({{type}} result) {
      _{{name}}SuccessCounter.incr();
    }
  }).onFailure(new Function<{{type}}, Void>() {
    public Void apply({{type}} result) {
      _{{name}}FailuresCounter.incr();
      _{{name}}FailuresScope.counter(ex.getClass.getName).incr();
    }
  });
}
