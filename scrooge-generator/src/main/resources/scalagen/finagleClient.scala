{{docstring}}
class FinagledClient(
  {{#hasParent}}override {{/hasParent}}val service: FinagleService[ThriftClientRequest, Array[Byte]],
  {{#hasParent}}override {{/hasParent}}val protocolFactory: TProtocolFactory = new TBinaryProtocol.Factory,
  {{#hasParent}}override {{/hasParent}}val serviceName: String = "",
  stats: StatsReceiver = NullStatsReceiver
) extends {{#hasParent}}{{finagleClientParent}}(service, protocolFactory, serviceName, stats) with {{/hasParent}}FutureIface {
{{^hasParent}}
  // ----- boilerplate that should eventually be moved into finagle:

  protected def encodeRequest(name: String, args: ThriftStruct) = {
    val buf = new TMemoryBuffer(512)
    val oprot = protocolFactory.getProtocol(buf)

    oprot.writeMessageBegin(new TMessage(name, TMessageType.CALL, 0))
    args.write(oprot)
    oprot.writeMessageEnd()

    val bytes = Arrays.copyOfRange(buf.getArray, 0, buf.length)
    new ThriftClientRequest(bytes, false)
  }

  protected def decodeResponse[T <: ThriftStruct](resBytes: Array[Byte], codec: ThriftStructCodec[T]) = {
    val iprot = protocolFactory.getProtocol(new TMemoryInputTransport(resBytes))
    val msg = iprot.readMessageBegin()
    try {
      if (msg.`type` == TMessageType.EXCEPTION) {
        val exception = TApplicationException.read(iprot) match {
          case sourced: SourcedException =>
            if (serviceName != "") sourced.serviceName = serviceName
            sourced
          case e => e
        }
        throw exception
      } else {
        codec.decode(iprot)
      }
    } finally {
      iprot.readMessageEnd()
    }
  }

  protected def missingResult(name: String) = {
    new TApplicationException(
      TApplicationException.MISSING_RESULT,
      name + " failed: unknown result"
    )
  }

  // ----- end boilerplate.

{{/hasParent}}
  private[this] val scopedStats = if (serviceName != "") stats.scope(serviceName) else stats
{{#functions}}
  {{>function}}
{{/function}}
}
