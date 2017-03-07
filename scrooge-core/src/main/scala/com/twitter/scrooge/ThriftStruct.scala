package com.twitter.scrooge

import org.apache.thrift.protocol.{TType, TProtocol}

trait ThriftStruct {
  @throws(classOf[org.apache.thrift.TException])
  def write(oprot: TProtocol)
}

trait HasThriftStructCodec3[T <: ThriftStruct] {
  def _codec: ThriftStructCodec3[T]
}

trait ThriftResponse[Result] {
  def successField: Option[Result]
  def exceptionFields: Iterable[Option[ThriftException]]
  /**
   * Return the first nonempty exception field.
   */
  def firstException(): Option[ThriftException] =
    exceptionFields.collectFirst(ThriftResponse.exceptionIsDefined)
}

object ThriftResponse {
  private val exceptionIsDefined: PartialFunction[Option[ThriftException], ThriftException] = {
    case Some(exception) => exception
  }
}

/**
 * Unions are tagged with this trait as well as with [[ThriftStruct]].
 */
trait ThriftUnion {
  /**
   * The type of the value contained in the union field.
   *
   * For unknown union fields, this will be `Unit`.
   *
   * @see the `ContainedType` type param on [[ThriftUnionFieldInfo]]
   */
  protected type ContainedType

  /**
   * The value of this union field.
   *
   * For unknown union fields, this will be `()`, the instance of `Unit`.
   */
  def containedValue(): ContainedType

  /**
   * The [[ThriftStructFieldInfo]] for this part of union.
   *
   * Returns `None` if this represents an unknown union field.
   */
  def unionStructFieldInfo: Option[ThriftStructFieldInfo]
}

/**
 * A trait encapsulating the logic for encoding and decoding a specific thrift struct
 * type.
 */
trait ThriftStructCodec[T <: ThriftStruct] {
  @throws(classOf[org.apache.thrift.TException])
  def encode(t: T, oprot: TProtocol): Unit

  @throws(classOf[org.apache.thrift.TException])
  def decode(iprot: TProtocol): T

  lazy val metaData: ThriftStructMetaData[T] = new ThriftStructMetaData(this)
}

/**
 * Introduced as a backwards compatible API bridge in Scrooge 3.
 * Scala generated structs extend from this class.
 *
 * @see [[ThriftStructCodec]]
 */
abstract class ThriftStructCodec3[T <: ThriftStruct] extends ThriftStructCodec[T] {
  protected def ttypeToString(byte: Byte): String = {
    // from https://github.com/apache/thrift/blob/master/lib/java/src/org/apache/thrift/protocol/TType.java
    byte match {
      case TType.STOP   => "STOP"
      case TType.VOID   => "VOID"
      case TType.BOOL   => "BOOL"
      case TType.BYTE   => "BYTE"
      case TType.DOUBLE => "DOUBLE"
      case TType.I16    => "I16"
      case TType.I32    => "I32"
      case TType.I64    => "I64"
      case TType.STRING => "STRING"
      case TType.STRUCT => "STRUCT"
      case TType.MAP    => "MAP"
      case TType.SET    => "SET"
      case TType.LIST   => "LIST"
      case TType.ENUM   => "ENUM"
      case _            => "UNKNOWN"
    }
  }
}

/**
 * Metadata for a method for a Thrift service.
 *
 * Comments below will use this example IDL:
 * {{{
 * service ExampleService {
 *   i32 boringMethod(
 *     1: i32 input1,
 *     2: string input2
 *   )
 * }
 * }}}
 */
trait ThriftMethod {
  /**
   * A struct wrapping method arguments
   *
   * For Scala generated code this will be a wrapper around all of the inputs.
   * Roughly:
   * {{{
   * class Args(input1: Int, input2: String) extends ThriftStruct
   * }}}
   */
  type Args <: ThriftStruct

  /**
   * The successful return type
   *
   * For Scala generated code this will be the response's type.
   * Roughly:
   * {{{
   * type SuccessType = Int
   * }}}
   */
  type SuccessType

  /** Contains success or thrift application exceptions */
  type Result <: ThriftResponse[SuccessType] with ThriftStruct

  // Note there is some indirection here for `FunctionType`, `ServiceType`
  // and `ServiceIfaceServiceType`. This is because for Scala generated with
  // Finagle bindings, these add dependencies on Twitter util and Finagle.
  // This indirection allows us to sidestep that and keep scrooge-core
  // free of those dependencies.

  /**
   * The type of this method, as a function.
   *
   * For Scala generated code with Finagle bindings this will be roughly:
   * {{{
   * Function2[Int, String, Future[Int]]
   * }}}
   *
   * For Scala generated code without Finagle bindings, this will be `Nothing`.
   */
  type FunctionType

  /**
   * The type of this method, as a Finagle `Service` from `Args` to
   * `Result`.
   *
   * For Scala generated code with Finagle bindings this will be roughly:
   * `Service[Args, Result]`.
   *
   * For Scala generated code without Finagle bindings, this will be `Nothing`.
   *
   * @see [[ServiceIfaceServiceType]] for a more ergonomic API.
   */
  type ServiceType

  /**
   * The type of this method, as a Finagle `Service` from `Args` to
   * `SuccessType`.
   *
   * For Scala generated code with Finagle bindings this will be roughly:
   * `Service[Args, SuccessType]`.
   *
   * For Scala generated code without Finagle bindings, this will be `Nothing`.
   */
  type ServiceIfaceServiceType

  /**
   * Convert a function implementation of this method into a
   * ServiceIface `Service` implementation returning `SuccessType`.
   *
   * For Scala generated code without Finagle bindings, this will not implemented.
   */
  def toServiceIfaceService(f: FunctionType): ServiceIfaceServiceType

  /**
   * Convert a function implementation of this method into a
   * `Service` implementation returning `Result`.
   *
   * For Scala generated code without Finagle bindings, this will not implemented.
   */
  def functionToService(f: FunctionType): ServiceType

  /**
   * Convert a service implementation of this method into a function implementation
   *
   * For Scala generated code without Finagle bindings, this will not implemented.
   */
  def serviceToFunction(svc: ServiceType): FunctionType

  /** Thrift annotations (user-defined key-value metadata) on the method */
  def annotations: Map[String, String]
  /** Thrift method name */
  def name: String

  /** Thrift service name. A thrift service is a list of methods. */
  def serviceName: String

  /** Codec for the request args */
  def argsCodec: ThriftStructCodec3[Args]

  /** Codec for the response */
  def responseCodec: ThriftStructCodec3[Result]

  /** True for oneway thrift methods */
  def oneway: Boolean
}

