// ----- finagle service

// import com.twitter.scrooge.FinagleThriftService;

class FinagledService extends {{extends}} {
  private FutureIface iface;
  private TProtocolFactory protocolFactory;

  public FinagledService(FutureIface iface, TProtocolFactory protocolFactory) {
    this.iface = iface;
    this.protocolFactory = protocolFactory;
  }
{{#functions}}
{{function}}
{{/function}}
}
