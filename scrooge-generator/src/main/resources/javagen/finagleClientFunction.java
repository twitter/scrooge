// FIXME
//private StatsReceiver _{{name}}Scope = scopedStats.scope("{{name}}");
//private Counter _{{name}}RequestsCounter = _{{name}}Scope.counter("requests");
//private Counter _{{name}}SuccessCounter = _{{name}}Scope.counter("success");
//private Counter _{{name}}FailuresCounter = _{{name}}Scope.counter("failures");
//private Counter _{{name}}FailuresScope = _{{name}}Scope.scope("failures");

{{#headerInfo}}{{>header}}{{/headerInfo}} {
//  _{{name}}RequestsCounter.incr();
  return this.service.apply(encodeRequest("{{name}}", new {{localName}}_args({{argNames}}))).flatMap(new Function<byte[], Future<{{type}}>>() {
    public Future<{{type}}> apply(byte[] in) {
      try {
        {{localName}}_result result = decodeResponse(in, {{localName}}_result.CODEC);

{{#hasThrows}}
        Exception exception = null;
{{#throws}}
        if (exception == null && result.{{name}}.isDefined()) exception = result.{{name}}.get();
{{/throws}}
        if (exception != null) return Future.exception(exception);
{{/hasThrows}}

{{#void}}
        return Future.value(null);
{{/void}}
{{^void}}
        if (result.success.isDefined()) return Future.value(result.success.get());
        return Future.exception(missingResult("{{name}}"));
{{/void}}
      } catch (TException e) {
        return Future.exception(e);
      }
    }
  }).rescue(new Function<Throwable, Future<{{type}}>>() {
      public Future<{{type}}> apply(Throwable t) {
        // if (throwable instanceof SourcedException) ex.serviceName = this.serviceName;
        return Future.exception(t);
      }
  })/*.onSuccess(new Function<{{type}}, Void>() {
    public Void apply({{type}} result) {
//      _{{name}}SuccessCounter.incr();
    }
  }).onFailure(new Function<Throwable, Void>() {
    public Void apply(Throwable ex) {
//      _{{name}}FailuresCounter.incr();
      // FIXME _{{name}}FailuresScope.counter(ex.getClass().getName()).incr();
    }
  })*/;
}
