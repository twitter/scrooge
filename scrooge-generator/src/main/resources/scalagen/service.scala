package {{package}}

import com.twitter.scrooge.{
  ThriftStruct, ThriftStructCodec, ThriftStructCodec3}
{{#enablePassthrough}}
import com.twitter.scrooge.ThriftUtil
{{/enablePassthrough}}
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import org.apache.thrift.protocol._
import org.apache.thrift.transport.TTransport
import org.apache.thrift.TApplicationException
import scala.collection.mutable
import scala.collection.{Map, Set}
{{#withFinagle}}
import com.twitter.util.Future
import com.twitter.conversions.time._
{{/withFinagle}}
{{#withFinagleClient}}
import com.twitter.finagle.{Service => FinagleService}
import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import com.twitter.finagle.thrift.ThriftClientRequest
import com.twitter.finagle.SourcedException
{{/withFinagleClient}}
{{#withFinagleService}}
import com.twitter.finagle.{Service => FinagleService}
import java.util.Arrays
import org.apache.thrift.transport.{TMemoryBuffer, TMemoryInputTransport, TTransport}
{{/withFinagleService}}
{{#withOstrichServer}}
import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.twitter.finagle.stats.{StatsReceiver, OstrichStatsReceiver}
import com.twitter.finagle.thrift.ThriftServerFramedCodec
import com.twitter.finagle.tracing.{NullTracer, Tracer}
import com.twitter.logging.Logger
import com.twitter.ostrich.admin.Service
import com.twitter.util.Duration
import java.util.concurrent.atomic.AtomicReference
{{/withOstrichServer}}

{{docstring}}
@javax.annotation.Generated(value = Array("com.twitter.scrooge.Compiler"), date = "{{date}}")
object {{ServiceName}} {
  trait Iface {{syncExtends}}{
{{#syncFunctions}}
    {{>function}}
{{/syncFunctions}}
  }

{{#withFinagle}}
  trait FutureIface {{asyncExtends}}{
{{#asyncFunctions}}
    {{>function}}
{{/asyncFunctions}}
  }
{{/withFinagle}}

{{#internalStructs}}
{{#internalArgsStruct}}
  {{>struct}}
{{/internalArgsStruct}}
{{#internalResultStruct}}
  {{>struct}}
{{/internalResultStruct}}
{{/internalStructs}}

{{#finagleClients}}
  {{>finagleClient}}
{{/finagleClients}}
{{#finagleServices}}
  {{>finagleService}}
{{/finagleServices}}
{{#ostrichServers}}
  {{>ostrichServer}}
{{/ostrichServers}}
}
