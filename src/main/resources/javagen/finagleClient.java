// ----- finagle client

//import com.twitter.finagle.{Service => FinagleService};
//import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
//import com.twitter.finagle.thrift.ThriftClientRequest;
//import com.twitter.scrooge.FinagleThriftClient;

class FinagledClient extends {{extends}} implements FutureIface {
  private FinagleService<ThriftClientRequest, Array<Byte>> service;
  private TProtocolFactory protocolFactory /* new TBinaryProtocol.Factory */;
  private StatsReceiver stats;

  public FinagledClient(
      FinagleService<ThriftClientRequest, Array<Byte>> service,
      TProtocolFactory protocolFactory /* new TBinaryProtocol.Factory */,
      StatsReceiver stats) {
    this.service = service;
    this.protocolFactory = protocolFactory;
    this.stats = stats;
  }

{{#functions}}
{{function}}
{{/function}}
}
