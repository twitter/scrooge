package {{package}}

import com.twitter.scrooge.{
  ThriftService, ThriftStruct, ThriftStructCodec, ThriftStructCodec3}
{{#enablePassthrough}}
import com.twitter.scrooge.ThriftUtil
{{/enablePassthrough}}
import java.nio.ByteBuffer
import org.apache.thrift.protocol._
import org.apache.thrift.transport.TTransport
import org.apache.thrift.TApplicationException
import org.apache.thrift.transport.TMemoryBuffer
import scala.collection.mutable
import scala.collection.{Map, Set}

{{docstring}}
@javax.annotation.Generated(value = Array("com.twitter.scrooge.Compiler"), date = "{{date}}")
trait {{ServiceName}}[+MM[_]] {{#genericParent}}extends {{genericParent}} {{/genericParent}}{
{{#genericFunctions}}
  {{>function}}
{{/genericFunctions}}
}

{{docstring}}
object {{ServiceName}} {
{{#internalStructs}}
{{#internalArgsStruct}}
  {{>struct}}
{{/internalArgsStruct}}
{{#internalResultStruct}}
  {{>struct}}
{{/internalResultStruct}}
{{/internalStructs}}

{{#withFinagle}}
  import com.twitter.util.Future

  @deprecated("use {{ServiceName}}[Future]", "3.4.0")
  trait FutureIface extends {{#futureIfaceParent}}{{futureIfaceParent}} with{{/futureIfaceParent}} {{ServiceName}}[Future] {
{{#asyncFunctions}}
    {{>function}}
{{/asyncFunctions}}
  }

  @deprecated("use {{ServiceName}}$FinagleClient", "3.4.0")
  class FinagledClient(
      service: com.twitter.finagle.Service[com.twitter.finagle.thrift.ThriftClientRequest, Array[Byte]],
      protocolFactory: TProtocolFactory = new TBinaryProtocol.Factory,
      serviceName: String = "",
      stats: com.twitter.finagle.stats.StatsReceiver = com.twitter.finagle.stats.NullStatsReceiver)
    extends {{ServiceName}}$FinagleClient(
      service,
      protocolFactory,
      serviceName,
      stats)
    with FutureIface

  @deprecated("use {{ServiceName}}$FinagleService", "3.4.0")
  class FinagledService(
      iface: FutureIface,
      protocolFactory: TProtocolFactory)
    extends {{ServiceName}}$FinagleService(
      iface,
      protocolFactory)
{{/withFinagle}}
}
