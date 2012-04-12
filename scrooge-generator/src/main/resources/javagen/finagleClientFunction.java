private StatsReceiver _{{name}}Scope = scopedStats.scope("{{name}}");
private Counter _{{name}}RequestsCounter = _{{name}}Scope.counter("requests");
private Counter _{{name}}SuccessCounter = _{{name}}Scope.counter("success");
private Counter _{{name}}FailuresCounter = _{{name}}Scope.counter("failures");
private Counter _{{name}}FailuresScope = _{{name}}Scope.scope("failures");

{{#headerInfo}}{{>header}}{{/headerInfo}} {
  _{{name}}RequestsCounter.incr();
  this.service.apply(encodeRequest("deliver", deliver_args(where))).flatMap(new Function<byte[], {{type}}>() {
    public {{type}} apply(byte[] in) {
      {{type}} result = decodeResponse(in, {{localName}}_result.decoder);

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
