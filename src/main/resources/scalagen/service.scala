package {{package}}

import com.twitter.conversions.time._
import com.twitter.finagle.SourcedException
import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec}
import com.twitter.util.Future
import java.net.InetSocketAddress
import org.apache.thrift.protocol._
import org.apache.thrift.TApplicationException
import scala.collection.mutable
import scala.collection.{Map, Set}
{{#finagleClient}}
import com.twitter.finagle.{Service => FinagleService}
import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import com.twitter.finagle.thrift.ThriftClientRequest
import com.twitter.scrooge.FinagleThriftClient
{{/finagleClient}}
{{#finagleService}}
import com.twitter.scrooge.FinagleThriftService
{{/finagleService}}
{{#ostrichServer}}
import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.twitter.finagle.stats.{StatsReceiver, OstrichStatsReceiver}
import com.twitter.finagle.thrift.ThriftServerFramedCodec
import com.twitter.finagle.tracing.{NullTracer, Tracer}
import com.twitter.logging.Logger
import com.twitter.ostrich.admin.Service
{{/ostrichServer}}

object {{name}} {
  trait Iface {{syncExtends}}{
{{#syncFunctions}}
    {{>function}}
{{/syncFunctions}}
  }

  trait FutureIface {{asyncExtends}}{
{{#asyncFunctions}}
    {{>function}}
{{/asyncFunctions}}
  }

{{functionStructs}}
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
