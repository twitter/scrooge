{{docstring}}
public static class FinagledClient{{#hasParent}} extends {{finagleClientParent}}{{/hasParent}} implements FutureIface {
  private com.twitter.finagle.Service<ThriftClientRequest, byte[]> service;
  private String serviceName;
  private TProtocolFactory protocolFactory /* new TBinaryProtocol.Factory */;
  private StatsReceiver scopedStats;

  public FinagledClient(
    com.twitter.finagle.Service<ThriftClientRequest, byte[]> service,
    TProtocolFactory protocolFactory /* new TBinaryProtocol.Factory */,
    String serviceName,
    StatsReceiver stats
  ) {
{{#hasParent}}
    super(service, protocolFactory, serviceName, stats);
{{/hasParent}}
    this.service = service;
    this.serviceName = serviceName;
    this.protocolFactory = protocolFactory;
    if (serviceName != "") {
      this.scopedStats = stats.scope(serviceName);
    } else {
      this.scopedStats = stats;
    }
  }

{{^hasParent}}
  // ----- boilerplate that should eventually be moved into finagle:

  protected ThriftClientRequest encodeRequest(String name, ThriftStruct args) {
    TMemoryBuffer buf = new TMemoryBuffer(512);
    TProtocol oprot = protocolFactory.getProtocol(buf);

    try {
      oprot.writeMessageBegin(new TMessage(name, TMessageType.CALL, 0));
      args.write(oprot);
      oprot.writeMessageEnd();
    } catch (TException e) {
      // not real.
    }

    byte[] bytes = Arrays.copyOfRange(buf.getArray(), 0, buf.length());
    return new ThriftClientRequest(bytes, false);
  }

  protected <T extends ThriftStruct> T decodeResponse(byte[] resBytes, ThriftStructCodec<T> codec) throws TException {
    TProtocol iprot = protocolFactory.getProtocol(new TMemoryInputTransport(resBytes));
    TMessage msg = iprot.readMessageBegin();
    try {
      if (msg.type == TMessageType.EXCEPTION) {
        TException exception = TApplicationException.read(iprot);
        if (exception instanceof SourcedException) {
          if (this.serviceName != "") ((SourcedException) exception).serviceName_$eq(this.serviceName);
        }
        throw exception;
      } else {
        return codec.decode(iprot);
      }
    } finally {
      iprot.readMessageEnd();
    }
  }

  protected Exception missingResult(String name) {
    return new TApplicationException(
      TApplicationException.MISSING_RESULT,
      "`" + name + "` failed: unknown result"
    );
  }

  protected class __Stats {
    public Counter requestsCounter, successCounter, failuresCounter;
    public StatsReceiver failuresScope;

    public __Stats(String name) {
      StatsReceiver scope = FinagledClient.this.scopedStats.scope(name);
      this.requestsCounter = scope.counter0("requests");
      this.successCounter = scope.counter0("success");
      this.failuresCounter = scope.counter0("failures");
      this.failuresScope = scope.scope("failures");
    }
  }

  // ----- end boilerplate.

{{/hasParent}}
{{#functions}}
  {{>function}}
{{/function}}
}
