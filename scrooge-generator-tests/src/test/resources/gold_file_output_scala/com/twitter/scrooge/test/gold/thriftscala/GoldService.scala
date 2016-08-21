/**
 * Generated by Scrooge
 *   version: ?
 *   rev: ?
 *   built at: ?
 */
package com.twitter.scrooge.test.gold.thriftscala

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
import com.twitter.finagle.{service => ctfs}
import com.twitter.finagle.thrift.{Protocols, ThriftClientRequest, ThriftServiceIface}
import com.twitter.util.Future
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


@javax.annotation.Generated(value = Array("com.twitter.scrooge.Compiler"))
trait GoldService[+MM[_]] extends ThriftService {
  /** Hello, I'm a comment. */
  def doGreatThings(request: com.twitter.scrooge.test.gold.thriftscala.Request): MM[com.twitter.scrooge.test.gold.thriftscala.Response]
}



object GoldService { self =>

  case class ServiceIface(
      doGreatThings : com.twitter.finagle.Service[self.DoGreatThings.Args, self.DoGreatThings.Result]
  ) extends BaseServiceIface

  // This is needed to support service inheritance.
  trait BaseServiceIface extends ToThriftService {
    def doGreatThings : com.twitter.finagle.Service[self.DoGreatThings.Args, self.DoGreatThings.Result]

    override def toThriftService: ThriftService = new MethodIface(this)
  }

  implicit object ServiceIfaceBuilder
    extends com.twitter.finagle.thrift.ServiceIfaceBuilder[ServiceIface] {
      def newServiceIface(
        binaryService: com.twitter.finagle.Service[ThriftClientRequest, Array[Byte]],
        pf: TProtocolFactory = Protocols.binaryFactory(),
        stats: com.twitter.finagle.stats.StatsReceiver
      ): ServiceIface =
        new ServiceIface(
          doGreatThings = ThriftServiceIface(self.DoGreatThings, binaryService, pf, stats)
      )
  }

  class MethodIface(serviceIface: BaseServiceIface)
    extends GoldService[Future] {
    private[this] val __doGreatThings_service =
      ThriftServiceIface.resultFilter(self.DoGreatThings) andThen serviceIface.doGreatThings
    def doGreatThings(request: com.twitter.scrooge.test.gold.thriftscala.Request): Future[com.twitter.scrooge.test.gold.thriftscala.Response] =
      __doGreatThings_service(self.DoGreatThings.Args(request))
  }

  implicit object MethodIfaceBuilder
    extends com.twitter.finagle.thrift.MethodIfaceBuilder[ServiceIface, GoldService[Future]] {
    def newMethodIface(serviceIface: ServiceIface): GoldService[Future] =
      new MethodIface(serviceIface)
  }

  object DoGreatThings extends com.twitter.scrooge.ThriftMethod {

    object Args extends ThriftStructCodec3[Args] {
      private val NoPassthroughFields = immutable$Map.empty[Short, TFieldBlob]
      val Struct = new TStruct("doGreatThings_args")
      val RequestField = new TField("request", TType.STRUCT, 1)
      val RequestFieldManifest = implicitly[Manifest[com.twitter.scrooge.test.gold.thriftscala.Request]]

      /**
       * Field information in declaration order.
       */
      lazy val fieldInfos: scala.List[ThriftStructFieldInfo] = scala.List[ThriftStructFieldInfo](
        new ThriftStructFieldInfo(
          RequestField,
          false,
          false,
          RequestFieldManifest,
          _root_.scala.None,
          _root_.scala.None,
          immutable$Map.empty[String, String],
          immutable$Map.empty[String, String],
          None
        )
      )

      lazy val structAnnotations: immutable$Map[String, String] =
        immutable$Map.empty[String, String]

      /**
       * Checks that all required fields are non-null.
       */
      def validate(_item: Args): Unit = {
      }

      def withoutPassthroughFields(original: Args): Args =
        new Args(
          request =
            {
              val field = original.request
              com.twitter.scrooge.test.gold.thriftscala.Request.withoutPassthroughFields(field)
            }
        )

      override def encode(_item: Args, _oproto: TProtocol): Unit = {
        _item.write(_oproto)
      }

      override def decode(_iprot: TProtocol): Args = {
        var request: com.twitter.scrooge.test.gold.thriftscala.Request = null
        var _passthroughFields: Builder[(Short, TFieldBlob), immutable$Map[Short, TFieldBlob]] = null
        var _done = false

        _iprot.readStructBegin()
        while (!_done) {
          val _field = _iprot.readFieldBegin()
          if (_field.`type` == TType.STOP) {
            _done = true
          } else {
            _field.id match {
              case 1 =>
                _field.`type` match {
                  case TType.STRUCT =>
                    request = readRequestValue(_iprot)
                  case _actualType =>
                    val _expectedType = TType.STRUCT
                    throw new TProtocolException(
                      "Received wrong type for field 'request' (expected=%s, actual=%s).".format(
                        ttypeToString(_expectedType),
                        ttypeToString(_actualType)
                      )
                    )
                }
              case _ =>
                if (_passthroughFields == null)
                  _passthroughFields = immutable$Map.newBuilder[Short, TFieldBlob]
                _passthroughFields += (_field.id -> TFieldBlob.read(_field, _iprot))
            }
            _iprot.readFieldEnd()
          }
        }
        _iprot.readStructEnd()

        new Args(
          request,
          if (_passthroughFields == null)
            NoPassthroughFields
          else
            _passthroughFields.result()
        )
      }

      def apply(
        request: com.twitter.scrooge.test.gold.thriftscala.Request
      ): Args =
        new Args(
          request
        )

      def unapply(_item: Args): _root_.scala.Option[com.twitter.scrooge.test.gold.thriftscala.Request] = _root_.scala.Some(_item.request)


      @inline private def readRequestValue(_iprot: TProtocol): com.twitter.scrooge.test.gold.thriftscala.Request = {
        com.twitter.scrooge.test.gold.thriftscala.Request.decode(_iprot)
      }

      @inline private def writeRequestField(request_item: com.twitter.scrooge.test.gold.thriftscala.Request, _oprot: TProtocol): Unit = {
        _oprot.writeFieldBegin(RequestField)
        writeRequestValue(request_item, _oprot)
        _oprot.writeFieldEnd()
      }

      @inline private def writeRequestValue(request_item: com.twitter.scrooge.test.gold.thriftscala.Request, _oprot: TProtocol): Unit = {
        request_item.write(_oprot)
      }


    }

    class Args(
        val request: com.twitter.scrooge.test.gold.thriftscala.Request,
        val _passthroughFields: immutable$Map[Short, TFieldBlob])
      extends ThriftStruct
      with _root_.scala.Product1[com.twitter.scrooge.test.gold.thriftscala.Request]
      with HasThriftStructCodec3[Args]
      with java.io.Serializable
    {
      import Args._
      def this(
        request: com.twitter.scrooge.test.gold.thriftscala.Request
      ) = this(
        request,
        Map.empty
      )

      def _1 = request



      override def write(_oprot: TProtocol): Unit = {
        Args.validate(this)
        _oprot.writeStructBegin(Struct)
        if (request ne null) writeRequestField(request, _oprot)
        if (_passthroughFields.nonEmpty) {
          _passthroughFields.values.foreach { _.write(_oprot) }
        }
        _oprot.writeFieldStop()
        _oprot.writeStructEnd()
      }

      def copy(
        request: com.twitter.scrooge.test.gold.thriftscala.Request = this.request,
        _passthroughFields: immutable$Map[Short, TFieldBlob] = this._passthroughFields
      ): Args =
        new Args(
          request,
          _passthroughFields
        )

      override def canEqual(other: Any): Boolean = other.isInstanceOf[Args]

      override def equals(other: Any): Boolean =
        canEqual(other) &&
          _root_.scala.runtime.ScalaRunTime._equals(this, other) &&
          _passthroughFields == other.asInstanceOf[Args]._passthroughFields

      override def hashCode: Int = _root_.scala.runtime.ScalaRunTime._hashCode(this)

      override def toString: String = _root_.scala.runtime.ScalaRunTime._toString(this)


      override def productArity: Int = 1

      override def productElement(n: Int): Any = n match {
        case 0 => this.request
        case _ => throw new IndexOutOfBoundsException(n.toString)
      }

      override def productPrefix: String = "Args"

      def _codec: ThriftStructCodec3[Args] = Args
    }

    type SuccessType = com.twitter.scrooge.test.gold.thriftscala.Response

    object Result extends ThriftStructCodec3[Result] {
      private val NoPassthroughFields = immutable$Map.empty[Short, TFieldBlob]
      val Struct = new TStruct("doGreatThings_result")
      val SuccessField = new TField("success", TType.STRUCT, 0)
      val SuccessFieldManifest = implicitly[Manifest[com.twitter.scrooge.test.gold.thriftscala.Response]]
      val ExField = new TField("ex", TType.STRUCT, 1)
      val ExFieldManifest = implicitly[Manifest[com.twitter.scrooge.test.gold.thriftscala.OverCapacityException]]

      /**
       * Field information in declaration order.
       */
      lazy val fieldInfos: scala.List[ThriftStructFieldInfo] = scala.List[ThriftStructFieldInfo](
        new ThriftStructFieldInfo(
          SuccessField,
          true,
          false,
          SuccessFieldManifest,
          _root_.scala.None,
          _root_.scala.None,
          immutable$Map.empty[String, String],
          immutable$Map.empty[String, String],
          None
        ),
        new ThriftStructFieldInfo(
          ExField,
          true,
          false,
          ExFieldManifest,
          _root_.scala.None,
          _root_.scala.None,
          immutable$Map.empty[String, String],
          immutable$Map.empty[String, String],
          None
        )
      )

      lazy val structAnnotations: immutable$Map[String, String] =
        immutable$Map.empty[String, String]

      /**
       * Checks that all required fields are non-null.
       */
      def validate(_item: Result): Unit = {
      }

      def withoutPassthroughFields(original: Result): Result =
        new Result(
          success =
            {
              val field = original.success
              field.map { field =>
                com.twitter.scrooge.test.gold.thriftscala.Response.withoutPassthroughFields(field)
              }
            },
          ex =
            {
              val field = original.ex
              field.map { field =>
                com.twitter.scrooge.test.gold.thriftscala.OverCapacityException.withoutPassthroughFields(field)
              }
            }
        )

      override def encode(_item: Result, _oproto: TProtocol): Unit = {
        _item.write(_oproto)
      }

      override def decode(_iprot: TProtocol): Result = {
        var success: _root_.scala.Option[com.twitter.scrooge.test.gold.thriftscala.Response] = _root_.scala.None
        var ex: _root_.scala.Option[com.twitter.scrooge.test.gold.thriftscala.OverCapacityException] = _root_.scala.None
        var _passthroughFields: Builder[(Short, TFieldBlob), immutable$Map[Short, TFieldBlob]] = null
        var _done = false

        _iprot.readStructBegin()
        while (!_done) {
          val _field = _iprot.readFieldBegin()
          if (_field.`type` == TType.STOP) {
            _done = true
          } else {
            _field.id match {
              case 0 =>
                _field.`type` match {
                  case TType.STRUCT =>
                    success = _root_.scala.Some(readSuccessValue(_iprot))
                  case _actualType =>
                    val _expectedType = TType.STRUCT
                    throw new TProtocolException(
                      "Received wrong type for field 'success' (expected=%s, actual=%s).".format(
                        ttypeToString(_expectedType),
                        ttypeToString(_actualType)
                      )
                    )
                }
              case 1 =>
                _field.`type` match {
                  case TType.STRUCT =>
                    ex = _root_.scala.Some(readExValue(_iprot))
                  case _actualType =>
                    val _expectedType = TType.STRUCT
                    throw new TProtocolException(
                      "Received wrong type for field 'ex' (expected=%s, actual=%s).".format(
                        ttypeToString(_expectedType),
                        ttypeToString(_actualType)
                      )
                    )
                }
              case _ =>
                if (_passthroughFields == null)
                  _passthroughFields = immutable$Map.newBuilder[Short, TFieldBlob]
                _passthroughFields += (_field.id -> TFieldBlob.read(_field, _iprot))
            }
            _iprot.readFieldEnd()
          }
        }
        _iprot.readStructEnd()

        new Result(
          success,
          ex,
          if (_passthroughFields == null)
            NoPassthroughFields
          else
            _passthroughFields.result()
        )
      }

      def apply(
        success: _root_.scala.Option[com.twitter.scrooge.test.gold.thriftscala.Response] = _root_.scala.None,
        ex: _root_.scala.Option[com.twitter.scrooge.test.gold.thriftscala.OverCapacityException] = _root_.scala.None
      ): Result =
        new Result(
          success,
          ex
        )

      def unapply(_item: Result): _root_.scala.Option[_root_.scala.Tuple2[Option[com.twitter.scrooge.test.gold.thriftscala.Response], Option[com.twitter.scrooge.test.gold.thriftscala.OverCapacityException]]] = _root_.scala.Some(_item.toTuple)


      @inline private def readSuccessValue(_iprot: TProtocol): com.twitter.scrooge.test.gold.thriftscala.Response = {
        com.twitter.scrooge.test.gold.thriftscala.Response.decode(_iprot)
      }

      @inline private def writeSuccessField(success_item: com.twitter.scrooge.test.gold.thriftscala.Response, _oprot: TProtocol): Unit = {
        _oprot.writeFieldBegin(SuccessField)
        writeSuccessValue(success_item, _oprot)
        _oprot.writeFieldEnd()
      }

      @inline private def writeSuccessValue(success_item: com.twitter.scrooge.test.gold.thriftscala.Response, _oprot: TProtocol): Unit = {
        success_item.write(_oprot)
      }

      @inline private def readExValue(_iprot: TProtocol): com.twitter.scrooge.test.gold.thriftscala.OverCapacityException = {
        com.twitter.scrooge.test.gold.thriftscala.OverCapacityException.decode(_iprot)
      }

      @inline private def writeExField(ex_item: com.twitter.scrooge.test.gold.thriftscala.OverCapacityException, _oprot: TProtocol): Unit = {
        _oprot.writeFieldBegin(ExField)
        writeExValue(ex_item, _oprot)
        _oprot.writeFieldEnd()
      }

      @inline private def writeExValue(ex_item: com.twitter.scrooge.test.gold.thriftscala.OverCapacityException, _oprot: TProtocol): Unit = {
        ex_item.write(_oprot)
      }


    }

    class Result(
        val success: _root_.scala.Option[com.twitter.scrooge.test.gold.thriftscala.Response],
        val ex: _root_.scala.Option[com.twitter.scrooge.test.gold.thriftscala.OverCapacityException],
        val _passthroughFields: immutable$Map[Short, TFieldBlob])
      extends ThriftResponse[com.twitter.scrooge.test.gold.thriftscala.Response] with ThriftStruct
      with _root_.scala.Product2[Option[com.twitter.scrooge.test.gold.thriftscala.Response], Option[com.twitter.scrooge.test.gold.thriftscala.OverCapacityException]]
      with HasThriftStructCodec3[Result]
      with java.io.Serializable
    {
      import Result._
      def this(
        success: _root_.scala.Option[com.twitter.scrooge.test.gold.thriftscala.Response] = _root_.scala.None,
        ex: _root_.scala.Option[com.twitter.scrooge.test.gold.thriftscala.OverCapacityException] = _root_.scala.None
      ) = this(
        success,
        ex,
        Map.empty
      )

      def _1 = success
      def _2 = ex

      def toTuple: _root_.scala.Tuple2[Option[com.twitter.scrooge.test.gold.thriftscala.Response], Option[com.twitter.scrooge.test.gold.thriftscala.OverCapacityException]] = {
        (
          success,
          ex
        )
      }

      def successField: Option[com.twitter.scrooge.test.gold.thriftscala.Response] = success
      def exceptionFields: Iterable[Option[com.twitter.scrooge.ThriftException]] = Seq(ex)


      override def write(_oprot: TProtocol): Unit = {
        Result.validate(this)
        _oprot.writeStructBegin(Struct)
        if (success.isDefined) writeSuccessField(success.get, _oprot)
        if (ex.isDefined) writeExField(ex.get, _oprot)
        if (_passthroughFields.nonEmpty) {
          _passthroughFields.values.foreach { _.write(_oprot) }
        }
        _oprot.writeFieldStop()
        _oprot.writeStructEnd()
      }

      def copy(
        success: _root_.scala.Option[com.twitter.scrooge.test.gold.thriftscala.Response] = this.success,
        ex: _root_.scala.Option[com.twitter.scrooge.test.gold.thriftscala.OverCapacityException] = this.ex,
        _passthroughFields: immutable$Map[Short, TFieldBlob] = this._passthroughFields
      ): Result =
        new Result(
          success,
          ex,
          _passthroughFields
        )

      override def canEqual(other: Any): Boolean = other.isInstanceOf[Result]

      override def equals(other: Any): Boolean =
        canEqual(other) &&
          _root_.scala.runtime.ScalaRunTime._equals(this, other) &&
          _passthroughFields == other.asInstanceOf[Result]._passthroughFields

      override def hashCode: Int = _root_.scala.runtime.ScalaRunTime._hashCode(this)

      override def toString: String = _root_.scala.runtime.ScalaRunTime._toString(this)


      override def productArity: Int = 2

      override def productElement(n: Int): Any = n match {
        case 0 => this.success
        case 1 => this.ex
        case _ => throw new IndexOutOfBoundsException(n.toString)
      }

      override def productPrefix: String = "Result"

      def _codec: ThriftStructCodec3[Result] = Result
    }

    type FunctionType = Function1[Args,Future[com.twitter.scrooge.test.gold.thriftscala.Response]]
    type ServiceType = com.twitter.finagle.Service[Args, Result]

    private[this] val toResult = (res: SuccessType) => Result(Some(res))

    def functionToService(f: FunctionType): ServiceType = {
      com.twitter.finagle.Service.mk { args: Args =>
        f(args).map(toResult)
      }
    }

    def serviceToFunction(svc: ServiceType): FunctionType = { args: Args =>
      ThriftServiceIface.resultFilter(this).andThen(svc).apply(args)
    }

    val name = "doGreatThings"
    val serviceName = "GoldService"
    val argsCodec = Args
    val responseCodec = Result
    val oneway = false
  }

  // Compatibility aliases.
  val doGreatThings$args = DoGreatThings.Args
  type doGreatThings$args = DoGreatThings.Args

  val doGreatThings$result = DoGreatThings.Result
  type doGreatThings$result = DoGreatThings.Result


  trait FutureIface extends GoldService[Future] {
    /** Hello, I'm a comment. */
    def doGreatThings(request: com.twitter.scrooge.test.gold.thriftscala.Request): Future[com.twitter.scrooge.test.gold.thriftscala.Response]
  }

  class FinagledClient(
      service: com.twitter.finagle.Service[ThriftClientRequest, Array[Byte]],
      protocolFactory: TProtocolFactory = Protocols.binaryFactory(),
      serviceName: String = "GoldService",
      stats: com.twitter.finagle.stats.StatsReceiver = com.twitter.finagle.stats.NullStatsReceiver,
      responseClassifier: ctfs.ResponseClassifier = ctfs.ResponseClassifier.Default)
    extends GoldService$FinagleClient(
      service,
      protocolFactory,
      serviceName,
      stats,
      responseClassifier)
    with FutureIface {

    def this(
      service: com.twitter.finagle.Service[ThriftClientRequest, Array[Byte]],
      protocolFactory: TProtocolFactory,
      serviceName: String,
      stats: com.twitter.finagle.stats.StatsReceiver
    ) = this(service, protocolFactory, serviceName, stats, ctfs.ResponseClassifier.Default)
  }

  class FinagledService(
      iface: FutureIface,
      protocolFactory: TProtocolFactory)
    extends GoldService$FinagleService(
      iface,
      protocolFactory)
}