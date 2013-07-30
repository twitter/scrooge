trait ThriftServer extends Service with FutureIface {
  val log = Logger.get(getClass)

  def thriftCodec = ThriftServerFramedCodec()
  def statsReceiver: StatsReceiver = new OstrichStatsReceiver
  def tracerFactory: Tracer.Factory = NullTracer.factory
  val thriftProtocolFactory: TProtocolFactory = new TBinaryProtocol.Factory()
  val thriftPort: Int
  val serverName: String

  // Must be thread-safe as different threads can start and shutdown the service.
  private[this] val _server = new AtomicReference[Server]
  def server = _server.get
  def server_=(value: Server) = _server.set(value)

  def start() {
    val thriftImpl = new FinagledService(this, thriftProtocolFactory)
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
      // TODO: once everyone in Twitter is on Finagle 6+, change this to
      // .tracer(tracerFactory())
      .tracerFactory(tracerFactory)

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
