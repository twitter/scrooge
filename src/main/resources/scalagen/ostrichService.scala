// ----- ostrich service

import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.twitter.finagle.stats.OstrichStatsReceiver
import com.twitter.finagle.thrift.ThriftServerFramedCodec
import com.twitter.logging.Logger
import com.twitter.ostrich.admin.Service

trait ThriftServer extends Service with FutureIface {
  val log = Logger.get(getClass)

  def thriftCodec = ThriftServerFramedCodec()
  val thriftProtocolFactory = new TBinaryProtocol.Factory()
  val thriftPort: Int
  val serverName: String

  var server: Server = null

  def start() {
    val thriftImpl = new FinagledService(this, thriftProtocolFactory)
    val serverAddr = new InetSocketAddress(thriftPort)
    server = ServerBuilder().codec(thriftCodec).name(serverName).reportTo(new OstrichStatsReceiver).bindTo(serverAddr).build(thriftImpl)
  }

  def shutdown() {
    synchronized {
      if (server != null) {
        server.close(0.seconds)
      }
    }
  }
}