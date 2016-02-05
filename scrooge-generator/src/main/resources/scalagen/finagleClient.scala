package {{package}}

import com.twitter.finagle.SourcedException
import com.twitter.finagle.{service => ctfs}
import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import com.twitter.finagle.thrift.{Protocols, ThriftClientRequest}
import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec}
import com.twitter.util.{Future, Return, Throw, Throwables}
import java.nio.ByteBuffer
import java.util.Arrays
import org.apache.thrift.protocol._
import org.apache.thrift.TApplicationException
import org.apache.thrift.transport.{TMemoryBuffer, TMemoryInputTransport}
import scala.collection.{Map, Set}
import scala.language.higherKinds

{{docstring}}
@javax.annotation.Generated(value = Array("com.twitter.scrooge.Compiler"))
class {{ServiceName}}$FinagleClient(
    {{#hasParent}}override {{/hasParent}}val service: com.twitter.finagle.Service[ThriftClientRequest, Array[Byte]],
    {{#hasParent}}override {{/hasParent}}val protocolFactory: TProtocolFactory,
    {{#hasParent}}override {{/hasParent}}val serviceName: String,
    stats: StatsReceiver,
    responseClassifier: ctfs.ResponseClassifier)
  extends {{#hasParent}}{{finagleClientParent}}(service, protocolFactory, serviceName, stats, responseClassifier) with {{/hasParent}}{{ServiceName}}[Future] {

  def this(
    service: com.twitter.finagle.Service[ThriftClientRequest, Array[Byte]],
    protocolFactory: TProtocolFactory = Protocols.binaryFactory(),
    serviceName: String = "{{ServiceName}}",
    stats: StatsReceiver = NullStatsReceiver
  ) = this(
    service,
    protocolFactory,
    serviceName,
    stats,
    ctfs.ResponseClassifier.Default
  )

  import {{ServiceName}}._
{{^hasParent}}

  protected def encodeRequest(name: String, args: ThriftStruct): ThriftClientRequest = {
    val buf = new TMemoryBuffer(512)
    val oprot = protocolFactory.getProtocol(buf)

    oprot.writeMessageBegin(new TMessage(name, TMessageType.CALL, 0))
    args.write(oprot)
    oprot.writeMessageEnd()

    val bytes = Arrays.copyOfRange(buf.getArray, 0, buf.length)
    new ThriftClientRequest(bytes, false)
  }

  protected def decodeResponse[T <: ThriftStruct](
    resBytes: Array[Byte],
    codec: ThriftStructCodec[T]
  ): T = {
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

  protected def setServiceName(ex: Throwable): Throwable =
    if (this.serviceName == "") ex
    else {
      ex match {
        case se: SourcedException =>
          se.serviceName = this.serviceName
          se
        case _ => ex
      }
    }

  // ----- end boilerplate.

{{/hasParent}}
  private[this] val scopedStats = if (serviceName != "") stats.scope(serviceName) else stats
{{#functions}}
  {{>finagleClientFunction}}
{{/function}}
}

