static class FinagledService extends {{parent}} {
  final private FutureIface iface;
  final private TProtocolFactory protocolFactory;

  public FinagledService(final FutureIface iface, final TProtocolFactory protocolFactory) {
{{#hasParent}}
    super(iface, protocolFactory);
{{/hasParent}}
{{^hasParent}}
    super(protocolFactory);
{{/hasParent}}
    this.iface = iface;
    this.protocolFactory = protocolFactory;

{{#functions}}
    {{>function}}
{{/function}}
  }
}
