class FinagledClient(
  service: FinagleService[ThriftClientRequest, Array[Byte]],
  protocolFactory: TProtocolFactory = new TBinaryProtocol.Factory,
  serviceName: String = "",
  stats: StatsReceiver = NullStatsReceiver
) extends {{parent}}{{#hasParent}}(service, protocolFactory, serviceName, stats){{/hasParent}}{{^hasParent}}(service, protocolFactory, serviceName){{/hasParent}} with FutureIface {
  private[this] val scopedStats = if (serviceName != "") stats.scope(serviceName) else stats
{{#functions}}
  {{>function}}
{{/function}}
}
