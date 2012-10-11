
private __Stats {{__stats_name}} = new __Stats("{{clientFuncName}}");

{{#headerInfo}}{{>header}}{{/headerInfo}} {
  {{__stats_name}}.requestsCounter.incr();

  Future<{{type}}> rv = this.service.apply(encodeRequest("{{clientFuncName}}", new {{ArgsStruct}}({{argNames}}))).flatMap(new Function<byte[], Future<{{type}}>>() {
    public Future<{{type}}> apply(byte[] in) {
      try {
        {{ResultStruct}} result = decodeResponse(in, {{ResultStruct}}.CODEC);

{{#hasThrows}}
        Exception exception = null;
{{#throws}}
        if (exception == null && result.{{throwName}}.isDefined()) exception = result.{{throwName}}.get();
{{/throws}}
        if (exception != null) return Future.exception(exception);
{{/hasThrows}}

{{#void}}
        return Future.value(null);
{{/void}}
{{^void}}
        if (result.success.isDefined()) return Future.value(result.success.get());
        return Future.exception(missingResult("{{clientFuncName}}"));
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
      {{__stats_name}}.successCounter.incr();
    }

    public void onFailure(Throwable t) {
      {{__stats_name}}.failuresCounter.incr();
      {{__stats_name}}.failuresScope.counter0(t.getClass().getName()).incr();
    }
  });

  return rv;
}
