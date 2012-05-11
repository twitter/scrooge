public static class FinagledClient extends {{parent}} implements FutureIface {
  private com.twitter.finagle.Service<ThriftClientRequest, byte[]> service;
  private TProtocolFactory protocolFactory /* new TBinaryProtocol.Factory */;
  private StatsReceiver scopedStats;

  public FinagledClient(
      com.twitter.finagle.Service<ThriftClientRequest, byte[]> service,
      TProtocolFactory protocolFactory /* new TBinaryProtocol.Factory */,
      String serviceName,
      StatsReceiver stats) {

{{#hasParent}}
    super(service, protocolFactory, serviceName, stats);
{{/hasParent}}
{{^hasParent}}
    super(service, protocolFactory, serviceName);
{{/hasParent}}
    this.service = service;
    this.protocolFactory = protocolFactory;
    if (serviceName != "") {
      this.scopedStats = stats.scope(serviceName);
    } else {
      this.scopedStats = stats;
    }
  }

{{#functions}}
  {{>function}}
{{/function}}
}
