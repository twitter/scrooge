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
      val (iprot, seqid) = request
      service(request).rescue {
        case e: TProtocolException => {
          iprot.readMessageEnd()
          Future.value(
            ProtocolException(
              null,
              exception("{{methodSvcNameForWire}}", seqid, TApplicationException.PROTOCOL_ERROR, e.getMessage),
              new TApplicationException(TApplicationException.PROTOCOL_ERROR, e.getMessage)))
        }
        case e: Exception => Future.exception(e)
      }
    }
  }

  val serdeFilter = new finagle$Filter[(TProtocol, Int), RichResponse[{{funcObjectName}}.Args, {{funcObjectName}}.Result], {{funcObjectName}}.Args, {{funcObjectName}}.SuccessType] {
    override def apply(
      request: (TProtocol, Int),
      service: finagle$Service[{{funcObjectName}}.Args, {{funcObjectName}}.SuccessType]
    ): Future[RichResponse[{{funcObjectName}}.Args, {{funcObjectName}}.Result]] = {
      val (iprot, seqid) = request
      val args = {{funcObjectName}}.Args.decode(iprot)
      iprot.readMessageEnd()
      val res = service(args)
      res.flatMap { value =>
        Future.value(
          SuccessfulResult(
            args,
            reply("{{methodSvcNameForWire}}", seqid, {{funcObjectName}}.Result({{resultNamedArg}})),
            {{funcObjectName}}.Result({{resultNamedArg}})))
      }.rescue {
{{#exceptions}}
        case e: {{exceptionType}} => {
          Future.value(
            ThriftExceptionResult(
              args,
              reply("{{methodSvcNameForWire}}", seqid, {{funcObjectName}}.Result({{fieldName}} = Some(e))),
              {{funcObjectName}}.Result({{fieldName}} = Some(e))))
        }
{{/exceptions}}
        case e => Future.exception(e)
      }
    }
  }

  statsFilter.andThen(protocolExnFilter).andThen(serdeFilter).andThen(methodService)
})
