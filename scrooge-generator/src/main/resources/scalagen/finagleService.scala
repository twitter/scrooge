
{{docstring}}
class FinagledService(
  iface: FutureIface,
  protocolFactory: TProtocolFactory
) extends {{finagleServiceParent}}{{#hasParent}}(iface, protocolFactory){{/hasParent}} {
{{^hasParent}}
  // ----- boilerplate that should eventually be moved into finagle:

  protected val functionMap = new mutable.HashMap[String, (TProtocol, Int) => Future[Array[Byte]]]()

  protected def addFunction(name: String, f: (TProtocol, Int) => Future[Array[Byte]]) {
    functionMap(name) = f
  }

  protected def exception(name: String, seqid: Int, code: Int, message: String): Future[Array[Byte]] = {
    try {
      val x = new TApplicationException(code, message)
      val memoryBuffer = new TMemoryBuffer(512)
      val oprot = protocolFactory.getProtocol(memoryBuffer)

      oprot.writeMessageBegin(new TMessage(name, TMessageType.EXCEPTION, seqid))
      x.write(oprot)
      oprot.writeMessageEnd()
      oprot.getTransport().flush()
      Future.value(Arrays.copyOfRange(memoryBuffer.getArray(), 0, memoryBuffer.length()))
    } catch {
      case e: Exception => Future.exception(e)
    }
  }

  protected def reply(name: String, seqid: Int, result: ThriftStruct): Future[Array[Byte]] = {
    try {
      val memoryBuffer = new TMemoryBuffer(512)
      val oprot = protocolFactory.getProtocol(memoryBuffer)

      oprot.writeMessageBegin(new TMessage(name, TMessageType.REPLY, seqid))
      result.write(oprot)
      oprot.writeMessageEnd()

      Future.value(Arrays.copyOfRange(memoryBuffer.getArray(), 0, memoryBuffer.length()))
    } catch {
      case e: Exception => Future.exception(e)
    }
  }

  final def apply(request: Array[Byte]): Future[Array[Byte]] = {
    val inputTransport = new TMemoryInputTransport(request)
    val iprot = protocolFactory.getProtocol(inputTransport)

    try {
      val msg = iprot.readMessageBegin()
      functionMap.get(msg.name) map { _.apply(iprot, msg.seqid) } getOrElse {
        TProtocolUtil.skip(iprot, TType.STRUCT)
        exception(msg.name, msg.seqid, TApplicationException.UNKNOWN_METHOD,
          "Invalid method name: '" + msg.name + "'")
      }
    } catch {
      case e: Exception => Future.exception(e)
    }
  }

  // ---- end boilerplate.

{{/hasParent}}
{{#functions}}
  {{>function}}
{{/function}}
}
