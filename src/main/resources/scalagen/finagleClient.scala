// ----- finagle client

import com.twitter.finagle.{Service => FinagleService}
import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import com.twitter.finagle.thrift.ThriftClientRequest
import com.twitter.scrooge.FinagleThriftClient

class FinagledClient(
  {{override}}val service: FinagleService[ThriftClientRequest, Array[Byte]],
  {{override}}val protocolFactory: TProtocolFactory = new TBinaryProtocol.Factory,
  override val serviceName: Option[String] = None,
  stats: StatsReceiver = NullStatsReceiver
) extends {{extends}} with FutureIface {
{{#functions}}
{{function}}
{{/function}}
}
