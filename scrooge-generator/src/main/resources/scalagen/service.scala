package {{package}}

import com.twitter.scrooge.{
  LazyTProtocol,
  HasThriftStructCodec3,
  TFieldBlob,
  ThriftService,
  ThriftStruct,
  ThriftStructCodec,
  ThriftStructCodec3,
  ThriftStructFieldInfo,
  ThriftResponse,
  ThriftUtil,
  ToThriftService
}
{{#withFinagle}}
import com.twitter.finagle.{service => ctfs}
import com.twitter.finagle.thrift.{
  Protocols,
  RichClientParam,
  RichServerParam,
  ThriftClientRequest,
  ThriftServiceIface
}
import com.twitter.util.Future
{{/withFinagle}}
import java.nio.ByteBuffer
import java.util.Arrays
import org.apache.thrift.protocol._
import org.apache.thrift.transport.TTransport
import org.apache.thrift.TApplicationException
import org.apache.thrift.transport.TMemoryBuffer
import scala.collection.immutable.{Map => immutable$Map}
import scala.collection.mutable.{
  Builder,
  ArrayBuffer => mutable$ArrayBuffer, Buffer => mutable$Buffer,
  HashMap => mutable$HashMap, HashSet => mutable$HashSet}
import scala.collection.{Map, Set}
import scala.language.higherKinds

{{docstring}}
@javax.annotation.Generated(value = Array("com.twitter.scrooge.Compiler"))
trait {{ServiceName}}[+MM[_]] {{#genericParent}}extends {{genericParent}} {{/genericParent}}{
{{#genericFunctions}}
  {{>function}}
{{/genericFunctions}}
}

{{docstring}}
object {{ServiceName}} { self =>

{{#annotations}}
  val annotations: immutable$Map[String, String] = immutable$Map(
{{#pairs}}
    "{{key}}" -> "{{value}}"
{{/pairs|,}}
  )
{{/annotations}}
{{^annotations}}
  val annotations: immutable$Map[String, String] = immutable$Map.empty
{{/annotations}}

{{#withFinagle}}
{{#generateServiceIface}}
  trait BaseServiceIface extends {{#parent}}{{parent}}.BaseServiceIface{{/parent}}{{^parent}}ToThriftService{{/parent}} {
{{#dedupedOwnFunctions}}
    def {{dedupedFuncName}} : com.twitter.finagle.Service[self.{{funcObjectName}}.Args, self.{{funcObjectName}}.SuccessType]
{{/dedupedOwnFunctions}}

    {{#parent}}override {{/parent}}def toThriftService: ThriftService = new MethodIface(this)
  }

  case class ServiceIface(
{{#inheritedFunctions}}
      {{funcName}} : com.twitter.finagle.Service[{{ParentServiceName}}.{{funcObjectName}}.Args, {{ParentServiceName}}.{{funcObjectName}}.SuccessType]
{{/inheritedFunctions|,}}
  ) extends {{#parent}}{{parent}}.BaseServiceIface
    with {{/parent}}BaseServiceIface
    with com.twitter.finagle.thrift.ThriftServiceIface.Filterable[ServiceIface] {

    /**
     * Prepends the given type-agnostic `Filter` to all of the `Services`
     * and returns a copy of the `ServiceIface` now including the filter.
     */
    def filtered(filter: com.twitter.finagle.Filter.TypeAgnostic): ServiceIface =
      copy(
{{#inheritedFunctions}}
        {{funcName}} = filter.toFilter.andThen({{funcName}})
{{/inheritedFunctions|,}}
      )
  }

  implicit object ServiceIfaceBuilder
    extends com.twitter.finagle.thrift.ServiceIfaceBuilder[ServiceIface] {
      def newServiceIface(
        binaryService: com.twitter.finagle.Service[ThriftClientRequest, Array[Byte]],
        clientParam: RichClientParam
      ): ServiceIface =
        ServiceIface(
{{#inheritedFunctions}}
          {{funcName}} = ThriftServiceIface({{ParentServiceName}}.{{funcObjectName}}, binaryService, clientParam)
{{/inheritedFunctions|,}}
        )
  }

  class MethodIface(serviceIface: BaseServiceIface)
    extends {{#parent}}{{parent}}.MethodIface(serviceIface)
    with {{/parent}}FutureIface {
{{#dedupedOwnFunctions}}
    def {{funcName}}({{fieldParams}}): Future[{{typeName}}] =
      serviceIface.{{dedupedFuncName}}(self.{{funcObjectName}}.Args({{argNames}})){{^isVoid}}{{/isVoid}}{{#isVoid}}.unit{{/isVoid}}
{{/dedupedOwnFunctions}}
  }

  implicit object MethodIfaceBuilder
    extends com.twitter.finagle.thrift.MethodIfaceBuilder[ServiceIface, {{ServiceName}}[Future]] {
    def newMethodIface(serviceIface: ServiceIface): MethodIface =
      new MethodIface(serviceIface)
  }
{{/generateServiceIface}}
{{^generateServiceIface}}
  // Skipped ServiceIface generation because this thrift service contains more than 254 methods.
  //
  // scalac 2.11 fails to compile classes with more than 254 method arguments
  // due to https://issues.scala-lang.org/browse/SI-7324.
{{/generateServiceIface}}

{{/withFinagle}}
{{#thriftFunctions}}
  object {{funcObjectName}} extends com.twitter.scrooge.ThriftMethod {
{{#functionArgsStruct}}
    {{>struct}}
{{/functionArgsStruct}}

    type SuccessType = {{typeName}}
{{#internalResultStruct}}
    {{>struct}}
{{/internalResultStruct}}

{{#annotations}}
    val annotations: immutable$Map[String, String] = immutable$Map(
{{#pairs}}
      "{{key}}" -> "{{value}}"
{{/pairs|,}}
    )
{{/annotations}}
{{^annotations}}
    val annotations: immutable$Map[String, String] = immutable$Map.empty
{{/annotations}}

{{#withFinagle}}
    type FunctionType = {{functionType}}
    type ServiceType = com.twitter.finagle.Service[Args, Result]

    type ServiceIfaceServiceType = com.twitter.finagle.Service[Args, SuccessType]

    def toServiceIfaceService(f: FunctionType): ServiceIfaceServiceType =
      com.twitter.finagle.Service.mk { args: Args =>
        f(args)
      }

    private[this] val toResult = (res: SuccessType) => Result({{^isVoid}}Some(res){{/isVoid}})

    def functionToService(f: FunctionType): ServiceType =
      com.twitter.finagle.Service.mk { args: Args =>
        f(args).map(toResult)
      }

    def serviceToFunction(svc: ServiceType): FunctionType = { args: Args =>
      com.twitter.finagle.thrift.ThriftServiceIface.resultFilter(this).andThen(svc).apply(args)
    }
{{/withFinagle}}
{{^withFinagle}}
    type FunctionType = Nothing
    type ServiceType = Nothing
    type ServiceIfaceServiceType = Nothing

    def toServiceIfaceService(f: FunctionType): ServiceIfaceServiceType = ???
    def functionToService(f: FunctionType): ServiceType = ???
    def serviceToFunction(svc: ServiceType): FunctionType = ???
{{/withFinagle}}

    val name: String = "{{originalFuncName}}"
    val serviceName: String = "{{ServiceName}}"
    val argsCodec = Args
    val responseCodec = Result
    val oneway: Boolean = {{is_oneway}}
  }

  // Compatibility aliases.
  val {{funcName}}$args = {{funcObjectName}}.Args
  type {{funcName}}$args = {{funcObjectName}}.Args

  val {{funcName}}$result = {{funcObjectName}}.Result
  type {{funcName}}$result = {{funcObjectName}}.Result

{{/thriftFunctions}}

{{#withFinagle}}
  trait FutureIface
    extends {{#futureIfaceParent}}{{futureIfaceParent}}
    with {{/futureIfaceParent}}{{ServiceName}}[Future] {
{{#asyncFunctions}}
    {{>function}}
{{/asyncFunctions}}
  }

  class FinagledClient(
      service: com.twitter.finagle.Service[ThriftClientRequest, Array[Byte]],
      clientParam: RichClientParam)
    extends {{ServiceName}}$FinagleClient(service, clientParam)
    with FutureIface {

    @deprecated("Use com.twitter.finagle.thrift.RichClientParam", "2017-08-16")
    def this(
      service: com.twitter.finagle.Service[ThriftClientRequest, Array[Byte]],
      protocolFactory: org.apache.thrift.protocol.TProtocolFactory = Protocols.binaryFactory(),
      serviceName: String = "{{ServiceName}}",
      stats: com.twitter.finagle.stats.StatsReceiver = com.twitter.finagle.stats.NullStatsReceiver,
      responseClassifier: ctfs.ResponseClassifier = ctfs.ResponseClassifier.Default
    ) = this(
      service,
      RichClientParam(
        protocolFactory,
        serviceName,
        clientStats = stats,
        responseClassifier = responseClassifier
      )
    )

    @deprecated("Use com.twitter.finagle.thrift.RichClientParam", "2017-08-16")
    def this(
      service: com.twitter.finagle.Service[ThriftClientRequest, Array[Byte]],
      protocolFactory: org.apache.thrift.protocol.TProtocolFactory,
      serviceName: String,
      stats: com.twitter.finagle.stats.StatsReceiver
    ) = this(
      service,
      RichClientParam(
        protocolFactory,
        serviceName,
        clientStats = stats
      )
    )
  }

  class FinagledService(
      iface: FutureIface,
      serverParam: RichServerParam)
    extends {{ServiceName}}$FinagleService(iface, serverParam) {

    @deprecated("Use com.twitter.finagle.thrift.RichServerParam", "2017-08-16")
    def this(
      iface: FutureIface,
      protocolFactory: org.apache.thrift.protocol.TProtocolFactory,
      serviceName: String = "{{ServiceName}}"
    ) = this(iface, RichServerParam(protocolFactory, serviceName))
  }
{{/withFinagle}}
}
