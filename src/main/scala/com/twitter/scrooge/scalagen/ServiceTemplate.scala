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
    val resultField = AST.Field(0, "success", f.`type`.asInstanceOf[FieldType], None, AST.Requiredness.Optional)
    AST.Struct(f.name + "_result", (resultField :: f.throws.toList))
  }

  def serviceFinagleFunctionExceptionFilter(f: Function, name: String, fill: String) = {
    if (f.throws.isEmpty) {
      ""
    } else {
      ", " + f.throws.map { t =>
        if (t.name == name) fill else "null"
      }.mkString(", ")
    }
  }

  def serviceFinagleFunctionException(function: Function, exceptionType: String, field: Field) = {
    "case e: " + exceptionType + " =>\n" +
      "  reply(\"" + function.name + "\", seqid, " + function.name + "_result(None" +
      serviceFinagleFunctionExceptionFilter(function, field.name, "e") + "))\n"
  }

  def clientFinagleFunctionTemplate = template[Function](
"""{{ futureFunctionTemplate(self, scope) }} = {
  encodeRequest("{{name}}", {{name}}_args({{ args.map(_.name).mkString(", ") }})) flatMap { this.service } flatMap {
    decodeResponse(_, {{name}}_result.decoder)
  } flatMap { result =>
    result.success.map(Future.value) getOrElse {
      Future.exception {
        {{ if (throws.isEmpty) "" else throws map { t => "Option(result." + t.name + ")" } mkString("", " orElse ", " getOrElse") }} { missingResult("{{name}}") }
      }
    }
  }
}
"""
  )

  val clientFinagleTemplate = template[Service](
"""// ----- finagle client

import com.twitter.scrooge.FinagleThriftClient
import com.twitter.finagle.{Service => FinagleService}
import com.twitter.finagle.thrift.ThriftClientRequest

class FinagledClient(
  val service: FinagleService[ThriftClientRequest, Array[Byte]],
  val protocolFactory: TProtocolFactory)
  extends FinagleThriftClient
  with FutureIface
{
{{ functions.map { f => clientFinagleFunctionTemplate(f, scope).indent }.mkString("\n") }}
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
      reply("{{name}}", seqid, {{name}}_result(Some(value){{ serviceFinagleFunctionExceptionFilter(self, "", "") }}))
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

import com.twitter.scrooge.FinagleThriftService

class FinagledService(iface: FutureIface, val protocolFactory: TProtocolFactory) extends FinagleThriftService {
{{ functions.map { f => serviceFinagleFunctionTemplate(f, scope).indent }.mkString("\n") }}
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
