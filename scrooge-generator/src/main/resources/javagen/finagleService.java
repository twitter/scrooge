class FinagledService extends {{parent}} {
  private FutureIface iface;
  private TProtocolFactory protocolFactory;

  public FinagledService(FutureIface iface, TProtocolFactory protocolFactory) {
{{#hasParent}}
    super(iface, protocolFactory);
{{/hasParent}}
    this.iface = iface;
    this.protocolFactory = protocolFactory;

{{#functions}}
  {{>function}}
{{/function}}
  }
}
