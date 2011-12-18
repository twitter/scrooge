// ----- finagle client

import com.twitter.finagle.{Service => FinagleService}
import com.twitter.finagle.thrift.ThriftClientRequest
import com.twitter.scrooge.FinagleThriftClient

class FinagledClient(
  {{override}}val service: FinagleService[ThriftClientRequest, Array[Byte]],
  {{override}}val protocolFactory: TProtocolFactory
) extends {{extends}} with FutureIface {
{{#functions}}
{{function}}
{{/function}}
}
