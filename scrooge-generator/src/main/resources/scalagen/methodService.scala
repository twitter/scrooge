addService("{{methodSvcNameForWire}}", {
  val statsFilter: finagle$Filter[(TProtocol, Int), Array[Byte], (TProtocol, Int), RichResponse[{{funcObjectName}}.Args, {{funcObjectName}}.Result]] = perMethodStatsFilter({{funcObjectName}})

  val methodService = new finagle$Service[{{funcObjectName}}.Args, {{funcObjectName}}.SuccessType] {
    def apply(args: {{funcObjectName}}.Args): Future[{{funcObjectName}}.SuccessType] = {
      iface.{{methodSvcNameForCompile}}({{argNames}})
    }
  }

  val protocolExnFilter = new SimpleFilter[(TProtocol, Int), RichResponse[{{funcObjectName}}.Args, {{funcObjectName}}.Result]] {
    def apply(
      request: (TProtocol, Int),
      service: finagle$Service[(TProtocol, Int), RichResponse[{{funcObjectName}}.Args, {{funcObjectName}}.Result]]
    ): Future[RichResponse[{{funcObjectName}}.Args, {{funcObjectName}}.Result]] = {
      val iprot = request._1
      val seqid = request._2
      val res = service(request)
      res.transform {
        case _root_.com.twitter.util.Throw(e: TProtocolException) =>
          iprot.readMessageEnd()
          Future.value(
            ProtocolException(
              null,
              exception("{{methodSvcNameForWire}}", seqid, TApplicationException.PROTOCOL_ERROR, e.getMessage),
              new TApplicationException(TApplicationException.PROTOCOL_ERROR, e.getMessage)))
        case _ =>
          res
      }
    }
  }

  val serdeFilter = new finagle$Filter[(TProtocol, Int), RichResponse[{{funcObjectName}}.Args, {{funcObjectName}}.Result], {{funcObjectName}}.Args, {{funcObjectName}}.SuccessType] {
    def apply(
      request: (TProtocol, Int),
      service: finagle$Service[{{funcObjectName}}.Args, {{funcObjectName}}.SuccessType]
    ): Future[RichResponse[{{funcObjectName}}.Args, {{funcObjectName}}.Result]] = {
      val iprot = request._1
      val seqid = request._2
      val args = {{funcObjectName}}.Args.decode(iprot)
      iprot.readMessageEnd()
      val res = service(args)
      res.transform {
        case _root_.com.twitter.util.Return(value) =>
          val methodResult = {{funcObjectName}}.Result({{resultNamedArg}})
          Future.value(
            SuccessfulResult(
              args,
              reply("{{methodSvcNameForWire}}", seqid, methodResult),
              methodResult))
{{#exceptions}}
        case _root_.com.twitter.util.Throw(e: {{exceptionType}}) => {
          val methodResult = {{funcObjectName}}.Result({{fieldName}} = Some(e))
          Future.value(
            ThriftExceptionResult(
              args,
              reply("{{methodSvcNameForWire}}", seqid, methodResult),
              methodResult))
        }
{{/exceptions}}
        case t @ _root_.com.twitter.util.Throw(_) =>
          Future.const(t.cast[RichResponse[{{funcObjectName}}.Args, {{funcObjectName}}.Result]])
      }
    }
  }

  statsFilter.andThen(protocolExnFilter).andThen(serdeFilter).andThen(methodService)
})
