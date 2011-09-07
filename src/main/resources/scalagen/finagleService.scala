// ----- finagle service

import com.twitter.scrooge.FinagleThriftService

class FinagledService(
  iface: FutureIface,
  val protocolFactory: TProtocolFactory
) extends {{extends}} {
{{#functions}}
{{function}}
{{/function}}
}
