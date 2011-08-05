package com.twitter.scrooge
package scalagen

import AST._

object ServiceTemplate extends ScalaTemplate {
  val functionThrowsTemplate = template[Field]("@throws(classOf[{{ scalaType(`type`) }}])")

  val functionDeclarationTemplate = template[Function](
"""def {{name}}({{ fieldArgs(args) }}): {{scalaType(`type`)}}""")

  val futureFunctionDeclarationTemplate = template[Function](
"""def {{name}}({{ fieldArgs(args) }}): Future[{{scalaType(`type`)}}]""")

  val functionTemplate = template[Function](
"""{{ throws.map { t => functionThrowsTemplate(t, scope) + "\n" }.mkString }}{{ functionDeclarationTemplate(self, scope) }}""")

  val futureFunctionTemplate = template[Function](
"""{{ throws.map { t => functionThrowsTemplate(t, scope) + "\n" }.mkString }}{{ futureFunctionDeclarationTemplate(self, scope) }}""")

  def serviceFunctionResultStruct(f: Function) = {
    val throws = f.throws map { _.copy(requiredness = Requiredness.Optional) }
    val success = f.`type` match {
      case Void => Nil
      case fieldType: FieldType =>
        Seq(AST.Field(0, "success", fieldType, None, AST.Requiredness.Optional))
    }
    AST.Struct(f.name + "_result", success ++ throws)
  }

  def serviceFinagleFunctionException(function: Function, exceptionType: String, field: Field) = {
    "case e: " + exceptionType + " =>\n" +
      "  reply(\"" + function.name + "\", seqid, " + function.name + "_result(" +
      field.name + " = Some(e)))"
  }

  def clientFinagleFunctionTemplate = template[Function](
"""{{ futureFunctionTemplate(self, scope) }} = {
  encodeRequest("{{name}}", {{name}}_args({{ args.map(_.name).mkString(", ") }})) flatMap { this.service } flatMap {
    decodeResponse(_, {{name}}_result.decoder)
  } flatMap { result =>
    {{ if (throws.isEmpty) "" else
         throws.map { "result." + _.name } mkString("(", " orElse ", ").map(Future.exception) getOrElse")
    }} {{ if (`type` eq AST.Void) "Future.Done" else
         "(result.success.map(Future.value) getOrElse missingResult(\""+name+"\"))" }}
  }
}
"""
  )

  val clientFinagleTemplate = template[Service](
"""// ----- finagle client

import com.twitter.finagle.{Service => FinagleService}
import com.twitter.finagle.thrift.ThriftClientRequest

class FinagledClient(
  val service: FinagleService[ThriftClientRequest, Array[Byte]],
  val protocolFactory: TProtocolFactory)
  extends FutureIface
{
{{ functions.map { f => clientFinagleFunctionTemplate(f, scope).indent }.mkString("\n") }}

  protected def encodeRequest(name: String, args: ThriftStruct): Future[ThriftClientRequest] = {
    Future {
      val buf = new TMemoryBuffer(512)
      val oprot = this.protocolFactory.getProtocol(buf)

      oprot.writeMessageBegin(new TMessage(name, TMessageType.CALL, 0))
      args.write(oprot)
      oprot.writeMessageEnd()

      val bytes = Arrays.copyOfRange(buf.getArray, 0, buf.length)
      new ThriftClientRequest(bytes, false)
    }
  }

  protected def decodeResponse[T](resBytes: Array[Byte], decoder: TProtocol => T): Future[T] = {
    Future {
      val iprot = protocolFactory.getProtocol(new TMemoryInputTransport(resBytes))
      val msg = iprot.readMessageBegin()
      try {
        if (msg.`type` == TMessageType.EXCEPTION) {
          throw TApplicationException.read(iprot)
        } else {
          decoder(iprot)
        }
      } finally {
        iprot.readMessageEnd()
      }
    }
  }

  protected def missingResult(name: String): Future[Nothing] = {
    Future.exception {
      new TApplicationException(
        TApplicationException.MISSING_RESULT,
        "`" + name + "` failed: unknown result")
    }
  }
}
"""
  )

  val serviceFinagleFunctionTemplate = template[Function](
"""functionMap("{{name}}") = { (iprot: TProtocol, seqid: Int) =>
  try {
    val args = {{name}}_args.decoder(iprot)
    iprot.readMessageEnd()
    (try {
      iface.{{name}}({{ args.map { a => "args." + a.name }.mkString(", ") }})
    } catch {
      case e: Exception => Future.exception(e)
    }) flatMap { value: {{scalaType(`type`)}} =>
      reply("{{name}}", seqid, {{name}}_result({{ if (`type` ne AST.Void) "success = Some(value)" else "" }}))
    } rescue {
{{ throws.map { t => serviceFinagleFunctionException(self, scalaType(t.`type`), t).indent(3) }.mkString("\n") }}
      case e: Throwable =>
        exception("{{name}}", seqid, TApplicationException.INTERNAL_ERROR, "Internal error processing {{name}}")
    }
  } catch {
    case e: TProtocolException =>
      iprot.readMessageEnd()
      exception("{{name}}", seqid, TApplicationException.PROTOCOL_ERROR, e.getMessage)
    case e: Exception =>
      Future.exception(e)
  }
}
""")

  val serviceFinagleTemplate = template[Service](
"""// ----- finagle service

import com.twitter.finagle.{Service => FinagleService}

class FinagledService(
  iface: FutureIface,
  val protocolFactory: TProtocolFactory)
  extends FinagleService[Array[Byte], Array[Byte]]
{
  protected val functionMap = new mutable.HashMap[String, (TProtocol, Int) => Future[Array[Byte]]]()

{{ functions.map { f => serviceFinagleFunctionTemplate(f, scope).indent }.mkString("\n") }}

  def exception(name: String, seqid: Int, code: Int, message: String): Future[Array[Byte]] = {
    Future {
      val x = new TApplicationException(code, message)
      val memoryBuffer = new TMemoryBuffer(512)
      val oprot = protocolFactory.getProtocol(memoryBuffer)

      oprot.writeMessageBegin(new TMessage(name, TMessageType.EXCEPTION, seqid))
      x.write(oprot)
      oprot.writeMessageEnd()
      oprot.getTransport.flush()
      Arrays.copyOfRange(memoryBuffer.getArray, 0, memoryBuffer.length)
    }
  }

  def reply(name: String, seqid: Int, result: ThriftStruct): Future[Array[Byte]] = {
    Future {
      val memoryBuffer = new TMemoryBuffer(512)
      val oprot = protocolFactory.getProtocol(memoryBuffer)

      oprot.writeMessageBegin(new TMessage(name, TMessageType.REPLY, seqid))
      result.write(oprot)
      oprot.writeMessageEnd()

      Arrays.copyOfRange(memoryBuffer.getArray, 0, memoryBuffer.length)
    }
  }

  def apply(request: Array[Byte]): Future[Array[Byte]] = {
    val inputTransport = new TMemoryInputTransport(request)
    val iprot = protocolFactory.getProtocol(inputTransport)

    try {
      val msg = iprot.readMessageBegin()
      functionMap.get(msg.name) match {
        case Some(f) =>
          f(iprot, msg.seqid)
        case None =>
          TProtocolUtil.skip(iprot, TType.STRUCT)
          iprot.readMessageEnd()
          exception(msg.name, msg.seqid, TApplicationException.UNKNOWN_METHOD, "Invalid method name: '" + msg.name + "'")
      }
    } catch {
      case e: Exception => Future.exception(e)
    }
  }
}
""")

  var serviceOstrichTemplate = template[Service](
"""// ----- ostrich service

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
""")

  val serviceTemplate = template[ScalaService](
"""object {{service.name}} {
  trait Iface {{ service.parent.map { "extends " + _ }.getOrElse("") }}{
{{ service.functions.map { f => functionTemplate(f, scope).indent(2) }.mkString("\n") }}
  }

  trait FutureIface {{ service.parent.map { "extends " + _ }.getOrElse("") }}{
{{ service.functions.map { f => futureFunctionTemplate(f, scope).indent(2) }.mkString("\n") }}
  }

{{
service.functions.map { f =>
  // generate a Struct for each function's args & retval
  structTemplate(AST.Struct(f.name + "_args", f.args), scope) + "\n" +
    structTemplate(serviceFunctionResultStruct(f), scope)
}.mkString("\n").indent
}}

{{ if (options contains WithFinagle) clientFinagleTemplate(service, scope).indent else "" }}

{{ if (options contains WithFinagle) serviceFinagleTemplate(service, scope).indent else "" }}

{{ if (options contains WithOstrich) serviceOstrichTemplate(service, scope).indent else "" }}
}
""")
}
