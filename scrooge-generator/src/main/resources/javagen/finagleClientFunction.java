
private __Stats __stats_{{name}} = new __Stats("{{name}}");

{{#headerInfo}}{{>header}}{{/headerInfo}} {
  __stats_{{name}}.requestsCounter.incr();

  Future<{{type}}> rv = this.service.apply(encodeRequest("{{name}}", new {{localName}}_args({{argNames}}))).flatMap(new Function<byte[], Future<{{type}}>>() {
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
      if (t instanceof SourcedException) {
        ((SourcedException) t).serviceName_$eq(FinagledClient.this.serviceName);
      }
      return Future.exception(t);
    }
  });

  rv.addEventListener(new FutureEventListener<{{type}}>() {
    public void onSuccess({{type}} result) {
      __stats_{{name}}.successCounter.incr();
    }

    public void onFailure(Throwable t) {
      __stats_{{name}}.failuresCounter.incr();
      __stats_{{name}}.failuresScope.counter0(t.getClass().getName()).incr();
    }
  });

  return rv;
}
