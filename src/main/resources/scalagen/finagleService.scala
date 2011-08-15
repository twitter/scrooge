// ----- finagle service

import com.twitter.scrooge.FinagleThriftService

class FinagledService(
  iface: FutureIface,
  val protocolFactory: TProtocolFactory)
  extends FinagleThriftService
{
{{#functions}}
{{function}}
{{/function}}
}