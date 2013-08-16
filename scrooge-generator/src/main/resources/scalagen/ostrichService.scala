package {{package}}

import com.twitter.conversions.time._
import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.stats.{StatsReceiver, OstrichStatsReceiver}
import com.twitter.finagle.thrift.ThriftServerFramedCodec
import com.twitter.finagle.tracing.{NullTracer, Tracer}
import com.twitter.logging.Logger
import com.twitter.ostrich
import com.twitter.util.{Duration, Future}
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference
import org.apache.thrift.protocol.{TBinaryProtocol, TProtocolFactory}
import org.apache.thrift.transport.TTransport

{{docstring}}
@deprecated("long-term, use twitter-server, short term, use com.twitter.scrooge.OstrichThriftServer", "3.3.3")
@javax.annotation.Generated(value = Array("com.twitter.scrooge.Compiler"), date = "{{date}}")
trait {{ServiceName}}$OstrichThriftServer extends ostrich.admin.Service
  with {{ServiceName}}[Future]
{
  val log = Logger.get(getClass)

  def thriftCodec = ThriftServerFramedCodec()
  def statsReceiver: StatsReceiver = new OstrichStatsReceiver
  def tracer: Tracer = tracerFactory()
  @deprecated("use tracer instead", "3.3.3")
  def tracerFactory: Tracer.Factory = NullTracer.factory
  val thriftProtocolFactory: TProtocolFactory = new TBinaryProtocol.Factory()
  val thriftPort: Int
  val serverName: String

  // Must be thread-safe as different threads can start and shutdown the service.
  private[this] val _server = new AtomicReference[Server]
  def server = _server.get
  def server_=(value: Server) = _server.set(value)

  def start() {
    val thriftImpl = new {{ServiceName}}$FinagleService(this, thriftProtocolFactory)
    server_=(serverBuilder.build(thriftImpl))
  }

  /**
   * You can override this to provide additional configuration
   * to the ServerBuilder.
   */
  def serverBuilder =
    ServerBuilder()
      .codec(thriftCodec)
      .name(serverName)
      .reportTo(statsReceiver)
      .bindTo(new InetSocketAddress(thriftPort))
      .tracer(tracer)

  /**
   * Close the underlying server gracefully with the given grace
   * period. close() will drain the current channels, waiting up to
   * ``timeout'', after which channels are forcibly closed.
   */
  def shutdown(timeout: Duration = 0.seconds) {
    synchronized {
      val s = server
      if (s != null) {
        s.close(timeout)
      }
    }
  }

  def shutdown() = shutdown(0.seconds)
}
