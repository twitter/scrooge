// ----- finagle client

import com.twitter.finagle.{Service => FinagleService}
import com.twitter.finagle.thrift.ThriftClientRequest
import com.twitter.scrooge.FinagleThriftClient

class FinagledClient(
  val service: FinagleService[ThriftClientRequest, Array[Byte]],
  val protocolFactory: TProtocolFactory)
  extends FinagleThriftClient with FutureIface
{
{{#functions}}
{{function}}
{{/function}}
}
