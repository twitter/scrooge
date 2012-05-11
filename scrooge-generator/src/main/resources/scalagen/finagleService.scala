class FinagledService(
  iface: FutureIface,
  protocolFactory: TProtocolFactory
) extends {{parent}}{{#hasParent}}(iface, protocolFactory){{/hasParent}}{{^hasParent}}(protocolFactory){{/hasParent}} {
{{#functions}}
  {{>function}}
{{/function}}
}
