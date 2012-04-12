class FinagledClient(
  {{#hasParent}}override {{/hasParent}}val service: FinagleService[ThriftClientRequest, Array[Byte]],
  {{#hasParent}}override {{/hasParent}}val protocolFactory: TProtocolFactory = new TBinaryProtocol.Factory,
  override val serviceName: Option[String] = None,
  stats: StatsReceiver = NullStatsReceiver
) extends {{parent}}{{#hasParent}}(service, protocolFactory){{/hasParent}} with FutureIface {
  private[this] val scopedStats = serviceName map { stats.scope(_) } getOrElse stats
{{#functions}}
  {{>function}}
{{/function}}
}
