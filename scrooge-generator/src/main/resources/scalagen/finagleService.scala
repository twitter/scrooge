class FinagledService(
  iface: FutureIface,
  {{#hasParent}}override {{/hasParent}}val protocolFactory: TProtocolFactory
) extends {{parent}}{{#hasParent}}(iface, protocolFactory){{/hasParent}} {
{{#functions}}
  {{>function}}
{{/function}}
}
