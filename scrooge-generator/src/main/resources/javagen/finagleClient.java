public static class FinagledClient{{#hasParent}} extends {{parent}}{{/hasParent}} implements FutureIface {
  private com.twitter.finagle.Service<ThriftClientRequest, byte[]> service;
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

    oprot.writeMessageBegin(new TMessage(name, TMessageType.CALL, 0));
    args.write(oprot);
    oprot.writeMessageEnd();

    byte[] bytes = Arrays.copyOfRange(buf.getArray(), 0, buf.length);
    return new ThriftClientRequest(bytes, false);
  }

  protected T decodeResponse<T extends ThriftStruct>(byte[] resBytes, ThriftStructCodec<T> codec) {
    TProtocol iprot = protocolFactory.getProtocol(new TMemoryInputTransport(resBytes));
    TMessage msg = iprot.readMessageBegin();
    try {
      if (msg.type == TMessageType.EXCEPTION) {
        Exception exception = TApplicationException.read(iprot);
        if (exception instanceof SourcedException) {
          if (serviceName != "") ((SourcedException) exception).serviceName = serviceName;
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
    )
  }

  // ----- end boilerplate.

{{/hasParent}}
{{#functions}}
  {{>function}}
{{/function}}
}
