addService("{{methodSvcNameForWire}}", {
  val statsFilter = perMethodStatsFilter({{funcObjectName}}, stats)

  val methodService = new finagle$Service[{{funcObjectName}}.Args, {{funcObjectName}}.SuccessType] {
    def apply(args: {{funcObjectName}}.Args): Future[{{funcObjectName}}.SuccessType] = {
      iface.{{methodSvcNameForCompile}}({{argNames}})
    }
  }

  val protocolExnFilter = new SimpleFilter[(TProtocol, Int), Array[Byte]] {
    def apply(
      request: (TProtocol, Int),
      service: finagle$Service[(TProtocol, Int), Array[Byte]]
    ): Future[Array[Byte]] = {
      val (iprot, seqid) = request
      service(request).rescue {
        case e: TProtocolException => {
          iprot.readMessageEnd()
          exception("{{methodSvcNameForWire}}", seqid, TApplicationException.PROTOCOL_ERROR, e.getMessage)
        }
        case e: Exception => Future.exception(e)
      }
    }
  }

  val serdeFilter = new finagle$Filter[(TProtocol, Int), Array[Byte], {{funcObjectName}}.Args, {{funcObjectName}}.SuccessType] {
    override def apply(
      request: (TProtocol, Int),
      service: finagle$Service[{{funcObjectName}}.Args, {{funcObjectName}}.SuccessType]
    ): Future[Array[Byte]] = {
      val (iprot, seqid) = request
      val args = {{funcObjectName}}.Args.decode(iprot)
      iprot.readMessageEnd()
      val res = service(args)
      res.flatMap { value =>
        reply("{{methodSvcNameForWire}}", seqid, {{funcObjectName}}.Result({{resultNamedArg}}))
      }.rescue {
{{#exceptions}}
        case e: {{exceptionType}} => {
          reply("{{methodSvcNameForWire}}", seqid, {{funcObjectName}}.Result({{fieldName}} = Some(e)))
        }
{{/exceptions}}
        case e => Future.exception(e)
      }
    }
  }

  protocolExnFilter.andThen(serdeFilter).andThen(statsFilter).andThen(methodService)
})
