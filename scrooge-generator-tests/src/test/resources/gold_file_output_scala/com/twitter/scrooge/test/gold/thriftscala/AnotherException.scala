/**
 * Generated by Scrooge
 *   version: ?
 *   rev: ?
 *   built at: ?
 */
package com.twitter.scrooge.test.gold.thriftscala

import com.twitter.io.Buf
import com.twitter.scrooge.{
  InvalidFieldsException,
  StructBuilder,
  StructBuilderFactory,
  TFieldBlob,
  ThriftStruct,
  ThriftStructField,
  ThriftStructFieldInfo,
  ThriftStructMetaData,
  ValidatingThriftStruct,
  ValidatingThriftStructCodec3
}
import org.apache.thrift.protocol._
import org.apache.thrift.transport.TMemoryBuffer
import scala.collection.immutable.{Map => immutable$Map}
import scala.collection.mutable.Builder
import scala.reflect.{ClassTag, classTag}


object AnotherException extends ValidatingThriftStructCodec3[AnotherException] with StructBuilderFactory[AnotherException] {
  private[this] val _protos: _root_.com.twitter.scrooge.internal.TProtocols = _root_.com.twitter.scrooge.internal.TProtocols()

  val Struct: TStruct = new TStruct("AnotherException")
  val ErrorCodeField: TField = new TField("errorCode", TType.I32, 1)
  val ErrorCodeFieldManifest: Manifest[Int] = manifest[Int]

  /**
   * Field information in declaration order.
   */
  lazy val fieldInfos: scala.List[ThriftStructFieldInfo] = scala.List[ThriftStructFieldInfo](
    new ThriftStructFieldInfo(
      ErrorCodeField,
      false,
      false,
      ErrorCodeFieldManifest,
      _root_.scala.None,
      _root_.scala.None,
      immutable$Map.empty[String, String],
      immutable$Map.empty[String, String],
      None,
      _root_.scala.Option(0)
    )
  )


  val structAnnotations: immutable$Map[String, String] =
    immutable$Map.empty[String, String]

  private val fieldTypes: IndexedSeq[ClassTag[_]] = IndexedSeq[ClassTag[_]](
    classTag[Int].asInstanceOf[ClassTag[_]]
  )

  private[this] val structFields: Seq[ThriftStructField[AnotherException]] = Seq[ThriftStructField[AnotherException]](
    new ThriftStructField[AnotherException](
      ErrorCodeField,
      _root_.scala.Some(ErrorCodeFieldManifest),
      classOf[AnotherException]) {
        def getValue[R](struct: AnotherException): R = struct.errorCode.asInstanceOf[R]
    }
  )

  override lazy val metaData: ThriftStructMetaData[AnotherException] =
    ThriftStructMetaData(this, structFields, fieldInfos, Nil, structAnnotations)

  /**
   * Checks that all required fields are non-null.
   */
  def validate(_item: AnotherException): Unit = {
  }

  /**
   * Checks that the struct is a valid as a new instance. If there are any missing required or
   * construction required fields, return a non-empty list.
   */
  def validateNewInstance(item: AnotherException): scala.Seq[com.twitter.scrooge.validation.Issue] = {
    val buf = scala.collection.mutable.ListBuffer.empty[com.twitter.scrooge.validation.Issue]

    buf ++= validateField(item.errorCode)
    buf.toList
  }

  /**
   * Validate that all validation annotations on the struct meet the criteria defined in the
   * corresponding [[com.twitter.scrooge.validation.ThriftConstraintValidator]].
   */
  def validateInstanceValue(item: AnotherException): Set[com.twitter.scrooge.validation.ThriftValidationViolation] = {
    val thriftValidator = com.twitter.scrooge.ThriftValidator()
    val violations = scala.collection.mutable.Set.empty[com.twitter.scrooge.validation.ThriftValidationViolation]
    violations ++= validateFieldValue("errorCode", item.errorCode, fieldInfos.apply(0).fieldAnnotations, thriftValidator);
    violations.toSet
  }

  def withoutPassthroughFields(original: AnotherException): AnotherException =
    new AnotherException(
      errorCode = original.errorCode
    )

  lazy val unsafeEmpty: AnotherException = {
    val errorCode: Int = 0

    new AnotherException(
      errorCode,
      _root_.com.twitter.scrooge.internal.TProtocols.NoPassthroughFields
    )
  }

  def newBuilder(): StructBuilder[AnotherException] = new AnotherExceptionStructBuilder(_root_.scala.None, fieldTypes)

  override def encode(_item: AnotherException, _oproto: TProtocol): Unit = {
    _item.write(_oproto)
  }


  override def decode(_iprot: TProtocol): AnotherException = {
    decodeInternal(_iprot, false)
  }

  private[this] def decodeInternal(_iprot: TProtocol, lazily: Boolean): AnotherException = {
    var errorCode: Int = 0

    var _passthroughFields: Builder[(Short, TFieldBlob), immutable$Map[Short, TFieldBlob]] = null
    var _done = false

    _iprot.readStructBegin()
    do {
      val _field = _iprot.readFieldBegin()
      val _fieldType = _field.`type`
      if (_fieldType == TType.STOP) {
        _done = true
      } else {
        _field.id match {
          case 1 =>
            _root_.com.twitter.scrooge.internal.TProtocols.validateFieldType(TType.I32, _fieldType, "errorCode")
            errorCode = _iprot.readI32()
          case _ =>
            _passthroughFields = _root_.com.twitter.scrooge.internal.TProtocols.readPassthroughField(_iprot, _field, _passthroughFields)
        }
        _iprot.readFieldEnd()
      }
    } while (!_done)
    _iprot.readStructEnd()


    val _passthroughFieldsResult =
      if (_passthroughFields eq null) _root_.com.twitter.scrooge.internal.TProtocols.NoPassthroughFields
      else _passthroughFields.result()
    new AnotherException(
      errorCode,
      _passthroughFieldsResult
    )
  }

  def apply(
    errorCode: Int
  ): AnotherException =
    new AnotherException(
      errorCode
    )

  def unapply(_item: AnotherException): _root_.scala.Some[Int] = _root_.scala.Some(_item.errorCode)



}

/**
 * Prefer the companion object's [[com.twitter.scrooge.test.gold.thriftscala.AnotherException.apply]]
 * for construction if you don't need to specify passthrough or
 * flags.
 */
class AnotherException(
    val errorCode: Int,
    val _passthroughFields: immutable$Map[Short, TFieldBlob],
    val flags: Long)
  extends _root_.com.twitter.scrooge.ThriftException with _root_.com.twitter.finagle.SourcedException with ThriftStruct
  with _root_.scala.Product1[Int]
  with ValidatingThriftStruct[AnotherException]
  with java.io.Serializable
  with _root_.com.twitter.finagle.FailureFlags[AnotherException]
{
  import AnotherException._

  def this(
    errorCode: Int,
    _passthroughFields: immutable$Map[Short, TFieldBlob]
  ) = this(
    errorCode,
    _passthroughFields,
    _root_.com.twitter.finagle.FailureFlags.Empty
  )

  def this(
    errorCode: Int
  ) = this(
    errorCode,
    immutable$Map.empty
  )

  def _1: Int = errorCode


  /**
   * Gets a field value encoded as a binary blob using TCompactProtocol.  If the specified field
   * is present in the passthrough map, that value is returned.  Otherwise, if the specified field
   * is known and not optional and set to None, then the field is serialized and returned.
   */
  def getFieldBlob(_fieldId: Short): _root_.scala.Option[TFieldBlob] = {
    val passedthroughValue = _passthroughFields.get(_fieldId)
    if (passedthroughValue.isDefined) {
      passedthroughValue
    } else {
      val _protos = _root_.com.twitter.scrooge.internal.TProtocols()
      val _buff = new TMemoryBuffer(32)
      val _oprot = new TCompactProtocol(_buff)

      val _fieldOpt: _root_.scala.Option[TField] = _fieldId match {
        case 1 =>
            _oprot.writeI32(errorCode)
            _root_.scala.Some(AnotherException.ErrorCodeField)
        case _ => _root_.scala.None
      }
      if (_fieldOpt.isDefined) {
        _root_.scala.Some(TFieldBlob(_fieldOpt.get, Buf.ByteArray.Owned(_buff.getArray)))
      } else {
        _root_.scala.None
      }
    }
  }


  /**
   * Collects TCompactProtocol-encoded field values according to `getFieldBlob` into a map.
   */
  def getFieldBlobs(ids: TraversableOnce[Short]): immutable$Map[Short, TFieldBlob] =
    (ids.flatMap { id => getFieldBlob(id).map { fieldBlob => (id, fieldBlob) } }).toMap

  /**
   * Sets a field using a TCompactProtocol-encoded binary blob.  If the field is a known
   * field, the blob is decoded and the field is set to the decoded value.  If the field
   * is unknown and passthrough fields are enabled, then the blob will be stored in
   * _passthroughFields.
   */
  def setField(_blob: TFieldBlob): AnotherException = {
    val _protos: _root_.com.twitter.scrooge.internal.TProtocols = _root_.com.twitter.scrooge.internal.TProtocols()
    var errorCode: Int = this.errorCode
    var _passthroughFields = this._passthroughFields
    val _iprot = _blob.read 
    _blob.id match {
      case 1 =>
        errorCode = _iprot.readI32()
      case _ => _passthroughFields += _root_.scala.Tuple2(_blob.id, _blob)
    }
    new AnotherException(
      errorCode,
      _passthroughFields
    )
  }

  /**
   * If the specified field is optional, it is set to None.  Otherwise, if the field is
   * known, it is reverted to its default value; if the field is unknown, it is removed
   * from the passthroughFields map, if present.
   */
  def unsetField(_fieldId: Short): AnotherException = {
    var errorCode: Int = this.errorCode

    _fieldId match {
      case 1 =>
        errorCode = 0
      case _ =>
    }
    new AnotherException(
      errorCode,
      _passthroughFields - _fieldId
    )
  }

  /**
   * If the specified field is optional, it is set to None.  Otherwise, if the field is
   * known, it is reverted to its default value; if the field is unknown, it is removed
   * from the passthroughFields map, if present.
   */
  def unsetErrorCode: AnotherException = unsetField(1)


  override def write(_oprot: TProtocol): Unit = {
    AnotherException.validate(this)
    val _protos = _root_.com.twitter.scrooge.internal.TProtocols()
    _oprot.writeStructBegin(Struct)
    _oprot.writeFieldBegin(ErrorCodeField)
    _oprot.writeI32(errorCode)
    _oprot.writeFieldEnd()
    _root_.com.twitter.scrooge.internal.TProtocols.finishWritingStruct(_oprot, _passthroughFields)
  }

  def copy(
    errorCode: Int = this.errorCode,
    _passthroughFields: immutable$Map[Short, TFieldBlob] = this._passthroughFields
  ): AnotherException =
    new AnotherException(
      errorCode,
      _passthroughFields
    )

  override def canEqual(other: Any): Boolean = other.isInstanceOf[AnotherException]

  private[this] def _equals(other: AnotherException): Boolean =
      this.productArity == other.productArity &&
      this.productIterator.sameElements(other.productIterator) &&
      this.flags == other.flags &&
      this._passthroughFields == other._passthroughFields

  override def equals(other: Any): Boolean =
    canEqual(other) && _equals(other.asInstanceOf[AnotherException])

  override def hashCode: Int = {
    31 * _root_.scala.runtime.ScalaRunTime._hashCode(this) +
      _root_.java.lang.Long.hashCode(this.flags)
  }

  override def toString: String = _root_.scala.runtime.ScalaRunTime._toString(this)

  override def productPrefix: String = "AnotherException"

  def _codec: ValidatingThriftStructCodec3[AnotherException] = AnotherException

  protected def copyWithFlags(flags: Long): AnotherException =
    new AnotherException(
      errorCode,
      _passthroughFields,
      flags
    )

  def newBuilder(): StructBuilder[AnotherException] = new AnotherExceptionStructBuilder(_root_.scala.Some(this), fieldTypes)
}

private[thriftscala] class AnotherExceptionStructBuilder(instance: _root_.scala.Option[AnotherException], fieldTypes: IndexedSeq[ClassTag[_]])
    extends StructBuilder[AnotherException](fieldTypes) {

  def build(): AnotherException = {
    val _fieldArray = fieldArray // shadow variable
    if (instance.isDefined) {
      val instanceValue = instance.get
      AnotherException(
        if (_fieldArray(0) == null) instanceValue.errorCode else _fieldArray(0).asInstanceOf[Int]
      )
    } else {
      if (genericArrayOps(_fieldArray).contains(null)) throw new InvalidFieldsException(structBuildError("AnotherException"))
      AnotherException(
        _fieldArray(0).asInstanceOf[Int]
      )
    }
  }
}

