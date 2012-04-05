public static class FinagledClient extends {{parent}} implements FutureIface {
  private FinagleService<ThriftClientRequest, Array<Byte>> service;
  private TProtocolFactory protocolFactory /* new TBinaryProtocol.Factory */;
  private StatsReceiver stats;

  public FinagledClient(
      FinagleService<ThriftClientRequest, Array<Byte>> service,
      TProtocolFactory protocolFactory /* new TBinaryProtocol.Factory */,
      StatsReceiver stats) {
  {{#hasParent}}
    super(service, protocolFactory, stats);
  {{/hasParent}}
    this.service = service;
    this.protocolFactory = protocolFactory;
    this.stats = stats;
  }

{{#functions}}
  {{>function}}
{{/function}}
}
