package {{package}}

import com.twitter.finagle.{SourcedException, Service => FinagleService}
import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import com.twitter.finagle.thrift.{Protocols, ThriftClientRequest}
import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec}
import com.twitter.util.{Future, Return, Throw}
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
  {{#hasParent}}override {{/hasParent}}val service: FinagleService[ThriftClientRequest, Array[Byte]],
  {{#hasParent}}override {{/hasParent}}val protocolFactory: TProtocolFactory = Protocols.binaryFactory(),
  {{#hasParent}}override {{/hasParent}}val serviceName: String = "",
  stats: StatsReceiver = NullStatsReceiver
) extends {{#hasParent}}{{finagleClientParent}}(service, protocolFactory, serviceName, stats) with {{/hasParent}}{{ServiceName}}[Future] {
  import {{ServiceName}}._
{{^hasParent}}

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

  protected def setServiceName(ex: Exception): Exception =
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
  {{>function}}
{{/function}}
}
