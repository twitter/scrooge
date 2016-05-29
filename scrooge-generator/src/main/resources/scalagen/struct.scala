{{#public}}
package {{package}}

import com.twitter.scrooge.{
  LazyTProtocol,
  TFieldBlob, ThriftException, ThriftStruct, ThriftStructCodec3, ThriftStructFieldInfo,
  ThriftStructMetaData, ThriftUtil}
import org.apache.thrift.protocol._
import org.apache.thrift.transport.{TMemoryBuffer, TTransport}
import java.nio.ByteBuffer
import java.util.Arrays
import scala.collection.immutable.{Map => immutable$Map}
import scala.collection.mutable.Builder
import scala.collection.mutable.{
  ArrayBuffer => mutable$ArrayBuffer, Buffer => mutable$Buffer,
  HashMap => mutable$HashMap, HashSet => mutable$HashSet}
import scala.collection.{Map, Set}

{{/public}}
{{docstring}}
object {{StructName}} extends ThriftStructCodec3[{{StructName}}] {
  private val NoPassthroughFields = immutable$Map.empty[Short, TFieldBlob]
  val Struct = new TStruct("{{StructNameForWire}}")
{{#fields}}
  val {{fieldConst}} = new TField("{{fieldNameForWire}}", TType.{{constType}}, {{id}})
{{#isEnum}}
  val {{fieldConst}}I32 = new TField("{{fieldNameForWire}}", TType.I32, {{id}})
{{/isEnum}}
  val {{fieldConst}}Manifest = implicitly[Manifest[{{fieldType}}]]
{{/fields}}

  /**
   * Field information in declaration order.
   */
  lazy val fieldInfos: scala.List[ThriftStructFieldInfo] = scala.List[ThriftStructFieldInfo](
{{#fields}}
    new ThriftStructFieldInfo(
      {{fieldConst}},
      {{optional}},
      {{required}},
      {{fieldConst}}Manifest,
{{#fieldKeyType}}
      _root_.scala.Some(implicitly[Manifest[{{fieldKeyType}}]]),
{{/fieldKeyType}}
{{^fieldKeyType}}
      _root_.scala.None,
{{/fieldKeyType}}
{{#fieldValueType}}
      _root_.scala.Some(implicitly[Manifest[{{fieldValueType}}]]),
{{/fieldValueType}}
{{^fieldValueType}}
      _root_.scala.None,
{{/fieldValueType}}
{{#fieldTypeAnnotations}}
      immutable$Map(
{{#pairs}}
        "{{key}}" -> "{{value}}"
{{/pairs|,}}
      ),
{{/fieldTypeAnnotations}}
{{^fieldTypeAnnotations}}
      immutable$Map.empty[String, String],
{{/fieldTypeAnnotations}}
{{#fieldFieldAnnotations}}
      immutable$Map(
{{#pairs}}
        "{{key}}" -> "{{value}}"
{{/pairs|,}}
      )
{{/fieldFieldAnnotations}}
{{^fieldFieldAnnotations}}
      immutable$Map.empty[String, String]
{{/fieldFieldAnnotations}}
    )
{{/fields|,}}
  )

  lazy val structAnnotations: immutable$Map[String, String] =
{{#structAnnotations}}
    immutable$Map[String, String](
{{#pairs}}
        "{{key}}" -> "{{value}}"
{{/pairs|,}}
    )
{{/structAnnotations}}
{{^structAnnotations}}
    immutable$Map.empty[String, String]
{{/structAnnotations}}

  /**
   * Checks that all required fields are non-null.
   */
  def validate(_item: {{StructName}}): Unit = {
{{#fields}}
{{#required}}
{{#nullable}}
    if (_item.{{fieldName}} == null) throw new TProtocolException("Required field {{fieldName}} cannot be null")
{{/nullable}}
{{/required}}
{{/fields}}
  }

  def withoutPassthroughFields(original: {{StructName}}): {{StructName}} =
    new {{InstanceClassName}}(
{{#fields}}
      {{fieldName}} =
        {
          val field = original.{{fieldName}}
          {{#passthroughFields}}{{>withoutPassthrough}}{{/passthroughFields}}
        }
{{/fields|,}}
    )

  override def encode(_item: {{StructName}}, _oproto: TProtocol): Unit = {
    _item.write(_oproto)
  }

{{#withTrait}}
  private[this] def lazyDecode(_iprot: LazyTProtocol): {{StructName}} = {

{{#fields}}
{{#isLazyReadEnabled}}
    var {{fieldNameForWire}}Offset: Int = -1
{{/isLazyReadEnabled}}
{{^isLazyReadEnabled}}
{{#optional}}
    var {{fieldName}}: Option[{{fieldType}}] = None
{{/optional}}
{{^optional}}
    var {{fieldName}}: {{fieldType}} = {{defaultReadValue}}
{{/optional}}
{{/isLazyReadEnabled}}
{{#required}}
    var {{gotName}} = false
{{/required}}
{{/fields}}

    var _passthroughFields: Builder[(Short, TFieldBlob), immutable$Map[Short, TFieldBlob]] = null
    var _done = false
    val _start_offset = _iprot.offset

    _iprot.readStructBegin()
    while (!_done) {
      val _field = _iprot.readFieldBegin()
      if (_field.`type` == TType.STOP) {
        _done = true
      } else {
        _field.id match {
{{#fields}}
          case {{id}} =>
            {{>readLazyField}}
{{/fields}}
          case _ =>
            if (_passthroughFields == null)
              _passthroughFields = immutable$Map.newBuilder[Short, TFieldBlob]
            _passthroughFields += (_field.id -> TFieldBlob.read(_field, _iprot))
        }
        _iprot.readFieldEnd()
      }
    }
    _iprot.readStructEnd()

{{#fields}}
{{#required}}
    if (!{{gotName}}) throw new TProtocolException("Required field '{{fieldName}}' was not found in serialized data for struct {{StructName}}")
{{/required}}
{{/fields}}
    new Lazy{{InstanceClassName}}(
      _iprot,
      _iprot.buffer,
      _start_offset,
      _iprot.offset,
{{#fields}}
      {{#isLazyReadEnabled}}{{fieldNameForWire}}Offset{{/isLazyReadEnabled}}{{^isLazyReadEnabled}}{{fieldName}}{{/isLazyReadEnabled}},
{{/fields}}
      if (_passthroughFields == null)
        NoPassthroughFields
      else
        _passthroughFields.result()
    )
  }

  override def decode(_iprot: TProtocol): {{StructName}} =
    _iprot match {
      case i: LazyTProtocol => lazyDecode(i)
      case i => eagerDecode(i)
    }

  private[this] def eagerDecode(_iprot: TProtocol): {{StructName}} = {
{{/withTrait}}
{{^withTrait}}
  override def decode(_iprot: TProtocol): {{StructName}} = {
{{/withTrait}}
{{#fields}}
{{#optional}}
    var {{fieldName}}: _root_.scala.Option[{{fieldType}}] = _root_.scala.None
{{/optional}}
{{^optional}}
    var {{fieldName}}: {{fieldType}} = {{defaultReadValue}}
{{#required}}
    var {{gotName}} = false
{{/required}}
{{/optional}}
{{/fields}}
    var _passthroughFields: Builder[(Short, TFieldBlob), immutable$Map[Short, TFieldBlob]] = null
    var _done = false

    _iprot.readStructBegin()
    while (!_done) {
      val _field = _iprot.readFieldBegin()
      if (_field.`type` == TType.STOP) {
        _done = true
      } else {
        _field.id match {
{{#fields}}
          case {{id}} =>
            {{>readField}}
{{/fields}}
          case _ =>
            if (_passthroughFields == null)
              _passthroughFields = immutable$Map.newBuilder[Short, TFieldBlob]
            _passthroughFields += (_field.id -> TFieldBlob.read(_field, _iprot))
        }
        _iprot.readFieldEnd()
      }
    }
    _iprot.readStructEnd()

{{#fields}}
{{#required}}
    if (!{{gotName}}) throw new TProtocolException("Required field '{{fieldName}}' was not found in serialized data for struct {{StructName}}")
{{/required}}
{{/fields}}
    new {{InstanceClassName}}(
{{#fields}}
      {{fieldName}},
{{/fields}}
      if (_passthroughFields == null)
        NoPassthroughFields
      else
        _passthroughFields.result()
    )
  }

  def apply(
{{#fields}}
    {{fieldName}}: {{>optionalType}}{{#hasDefaultValue}} = {{defaultFieldValue}}{{/hasDefaultValue}}{{#optional}} = _root_.scala.None{{/optional}}
{{/fields|,}}
  ): {{StructName}} =
    new {{InstanceClassName}}(
{{#fields}}
      {{fieldName}}
{{/fields|,}}
    )

{{#arity0}}
  def unapply(_item: {{StructName}}): Boolean = true
{{/arity0}}
{{#arity1}}
  def unapply(_item: {{StructName}}): _root_.scala.Option[{{>optionalType}}] = _root_.scala.Some(_item.{{fieldName}})
{{/arity1}}
{{#arityN}}
  def unapply(_item: {{StructName}}): _root_.scala.Option[{{product}}] = _root_.scala.Some(_item)
{{/arityN}}


{{#fields}}
  @inline private def {{readFieldValueName}}(_iprot: TProtocol): {{fieldType}} = {
{{#readWriteInfo}}
    {{>readValue}}
{{/readWriteInfo}}
  }

  @inline private def {{writeFieldName}}({{valueVariableName}}: {{fieldType}}, _oprot: TProtocol): Unit = {
{{#readWriteInfo}}
    _oprot.writeFieldBegin({{fieldConst}}{{#isEnum}}I32{{/isEnum}})
    {{writeFieldValueName}}({{valueVariableName}}, _oprot)
    _oprot.writeFieldEnd()
{{/readWriteInfo}}
  }

  @inline private def {{writeFieldValueName}}({{valueVariableName}}: {{fieldType}}, _oprot: TProtocol): Unit = {
{{#readWriteInfo}}
    {{>writeValue}}
{{/readWriteInfo}}
  }

{{/fields}}

{{#withTrait}}
  object Immutable extends ThriftStructCodec3[{{StructName}}] {
    override def encode(_item: {{StructName}}, _oproto: TProtocol): Unit = { _item.write(_oproto) }
    override def decode(_iprot: TProtocol): {{StructName}} = {{StructName}}.decode(_iprot)
    override lazy val metaData: ThriftStructMetaData[{{StructName}}] = {{StructName}}.metaData
  }

  /**
   * The default read-only implementation of {{StructName}}.  You typically should not need to
   * directly reference this class; instead, use the {{StructName}}.apply method to construct
   * new instances.
   */
  class Immutable(
{{#fields}}
      val {{fieldName}}: {{>optionalType}},
{{/fields}}
      override val _passthroughFields: immutable$Map[Short, TFieldBlob])
    extends {{StructName}} {
    def this(
{{#fields}}
      {{fieldName}}: {{>optionalType}}{{#hasDefaultValue}} = {{defaultFieldValue}}{{/hasDefaultValue}}{{#optional}} = _root_.scala.None{{/optional}}
{{/fields|,}}
    ) = this(
{{#fields}}
      {{fieldName}},
{{/fields}}
      Map.empty
    )
  }

  /**
   * This is another Immutable, this however keeps strings as lazy values that are lazily decoded from the backing
   * array byte on read.
   */
  private[this] class Lazy{{InstanceClassName}}(
      _proto: LazyTProtocol,
      _buf: Array[Byte],
      _start_offset: Int,
      _end_offset: Int,
{{#fields}}
      {{#isLazyReadEnabled}}{{fieldNameForWire}}Offset: Int,{{/isLazyReadEnabled}}{{^isLazyReadEnabled}}val {{fieldName}}: {{>optionalType}},{{/isLazyReadEnabled}}
{{/fields}}
      override val _passthroughFields: immutable$Map[Short, TFieldBlob])
    extends {{StructName}} {

    override def write(_oprot: TProtocol): Unit = {
      _oprot match {
        case i: LazyTProtocol => i.writeRaw(_buf, _start_offset, _end_offset - _start_offset)
        case _ => super.write(_oprot)
      }
    }

{{#fields}}
{{#isLazyReadEnabled}}
    lazy val {{fieldName}}: {{>optionalType}} =
{{#optional}}
      if ({{fieldNameForWire}}Offset == -1)
        None
      else {
        Some(_proto.{{decodeProtocol}}(_buf, {{fieldNameForWire}}Offset))
      }
{{/optional}}
{{^optional}}
      if ({{fieldNameForWire}}Offset == -1)
        {{defaultReadValue}}
      else {
        _proto.{{decodeProtocol}}(_buf, {{fieldNameForWire}}Offset)
      }
{{/optional}}
{{/isLazyReadEnabled}}
{{/fields}}

    /**
     * Override the super hash code to make it a lazy val rather than def.
     *
     * Calculating the hash code can be expensive, caching it where possible
     * can provide significant performance wins. (Key in a hash map for instance)
     * Usually not safe since the normal constructor will accept a mutable map or
     * set as an arg
     * Here however we control how the class is generated from serialized data.
     * With the class private and the contract that we throw away our mutable references
     * having the hash code lazy here is safe.
     */
    override lazy val hashCode = super.hashCode
  }

  /**
   * This Proxy trait allows you to extend the {{StructName}} trait with additional state or
   * behavior and implement the read-only methods from {{StructName}} using an underlying
   * instance.
   */
  trait Proxy extends {{StructName}} {
    protected def {{underlyingStructName}}: {{StructName}}
{{#fields}}
    override def {{fieldName}}: {{>optionalType}} = {{underlyingStructName}}.{{fieldName}}
{{/fields}}
    override def _passthroughFields = {{underlyingStructName}}._passthroughFields
  }
{{/withTrait}}
}

{{#withTrait}}
trait {{StructName}}
{{/withTrait}}
{{^withTrait}}
class {{StructName}}(
{{#fields}}
    val {{fieldName}}: {{>optionalType}},
{{/fields}}
    val _passthroughFields: immutable$Map[Short, TFieldBlob])
{{/withTrait}}
  extends {{parentType}}
  with {{product}}
  with java.io.Serializable
{
  import {{StructName}}._
{{^withTrait}}
  def this(
{{#fields}}
    {{fieldName}}: {{>optionalType}}{{#hasDefaultValue}} = {{defaultFieldValue}}{{/hasDefaultValue}}{{#optional}} = _root_.scala.None{{/optional}}
{{/fields|,}}
  ) = this(
{{#fields}}
    {{fieldName}},
{{/fields}}
    Map.empty
  )
{{/withTrait}}
{{#withTrait}}

{{#fields}}
  def {{fieldName}}: {{>optionalType}}
{{/fields}}

  def _passthroughFields: immutable$Map[Short, TFieldBlob] = immutable$Map.empty
{{/withTrait}}

{{#fields}}
  def _{{indexP1}} = {{fieldName}}
{{/fields}}

{{#isResponse}}
  def successField: Option[{{successFieldType}}] = {{successFieldValue}}
  def exceptionFields: Iterable[Option[com.twitter.scrooge.ThriftException]] = {{exceptionValues}}
{{/isResponse}}

{{#withFieldGettersAndSetters}}
  /**
   * Gets a field value encoded as a binary blob using TCompactProtocol.  If the specified field
   * is present in the passthrough map, that value is returned.  Otherwise, if the specified field
   * is known and not optional and set to None, then the field is serialized and returned.
   */
  def getFieldBlob(_fieldId: Short): _root_.scala.Option[TFieldBlob] = {
    lazy val _buff = new TMemoryBuffer(32)
    lazy val _oprot = new TCompactProtocol(_buff)
    _passthroughFields.get(_fieldId) match {
      case blob: _root_.scala.Some[TFieldBlob] => blob
      case _root_.scala.None => {
        val _fieldOpt: _root_.scala.Option[TField] =
          _fieldId match {
{{#fields}}
            case {{id}} =>
{{#readWriteInfo}}
{{#optional}}
              if ({{fieldName}}.isDefined) {
{{/optional}}
{{^optional}}
{{#nullable}}
              if ({{fieldName}} ne null) {
{{/nullable}}
{{^nullable}}
              if (true) {
{{/nullable}}
{{/optional}}
                {{writeFieldValueName}}({{fieldName}}{{#optional}}.get{{/optional}}, _oprot)
                _root_.scala.Some({{StructName}}.{{fieldConst}})
              } else {
                _root_.scala.None
              }
{{/readWriteInfo}}
{{/fields}}
            case _ => _root_.scala.None
          }
        _fieldOpt match {
          case _root_.scala.Some(_field) =>
            val _data = Arrays.copyOfRange(_buff.getArray, 0, _buff.length)
            _root_.scala.Some(TFieldBlob(_field, _data))
          case _root_.scala.None =>
            _root_.scala.None
        }
      }
    }
  }

  /**
   * Collects TCompactProtocol-encoded field values according to `getFieldBlob` into a map.
   */
  def getFieldBlobs(ids: TraversableOnce[Short]): immutable$Map[Short, TFieldBlob] =
    (ids flatMap { id => getFieldBlob(id) map { id -> _ } }).toMap

  /**
   * Sets a field using a TCompactProtocol-encoded binary blob.  If the field is a known
   * field, the blob is decoded and the field is set to the decoded value.  If the field
   * is unknown and passthrough fields are enabled, then the blob will be stored in
   * _passthroughFields.
   */
  def setField(_blob: TFieldBlob): {{StructName}} = {
{{#fields}}
    var {{fieldName}}: {{>optionalType}} = this.{{fieldName}}
{{/fields}}
    var _passthroughFields = this._passthroughFields
    _blob.id match {
{{#fields}}
{{#readWriteInfo}}
      case {{id}} =>
{{#optional}}
        {{fieldName}} = _root_.scala.Some({{readFieldValueName}}(_blob.read))
{{/optional}}
{{^optional}}
        {{fieldName}} = {{readFieldValueName}}(_blob.read)
{{/optional}}
{{/readWriteInfo}}
{{/fields}}
      case _ => _passthroughFields += (_blob.id -> _blob)
    }
    new {{InstanceClassName}}(
{{#fields}}
      {{fieldName}},
{{/fields}}
      _passthroughFields
    )
  }

  /**
   * If the specified field is optional, it is set to None.  Otherwise, if the field is
   * known, it is reverted to its default value; if the field is unknown, it is removed
   * from the passthroughFields map, if present.
   */
  def unsetField(_fieldId: Short): {{StructName}} = {
{{#fields}}
    var {{fieldName}}: {{>optionalType}} = this.{{fieldName}}
{{/fields}}

    _fieldId match {
{{#fields}}
      case {{id}} =>
{{#optional}}
        {{fieldName}} = _root_.scala.None
{{/optional}}
{{^optional}}
        {{fieldName}} = {{defaultReadValue}}
{{/optional}}
{{/fields}}
      case _ =>
    }
    new {{InstanceClassName}}(
{{#fields}}
      {{fieldName}},
{{/fields}}
      _passthroughFields - _fieldId
    )
  }

  /**
   * If the specified field is optional, it is set to None.  Otherwise, if the field is
   * known, it is reverted to its default value; if the field is unknown, it is removed
   * from the passthroughFields map, if present.
   */
{{#fields}}
  def {{unsetName}}: {{StructName}} = unsetField({{id}})

{{/fields}}
{{/withFieldGettersAndSetters}}

  override def write(_oprot: TProtocol): Unit = {
    {{StructName}}.validate(this)
    _oprot.writeStructBegin(Struct)
{{#fields}}
{{#readWriteInfo}}
{{#optional}}
    if ({{fieldName}}.isDefined) {{writeFieldName}}({{fieldName}}.get, _oprot)
{{/optional}}
{{^optional}}
{{#nullable}}
    if ({{fieldName}} ne null) {{writeFieldName}}({{fieldName}}, _oprot)
{{/nullable}}
{{^nullable}}
    {{writeFieldName}}({{fieldName}}, _oprot)
{{/nullable}}
{{/optional}}
{{/readWriteInfo}}
{{/fields}}
    if (_passthroughFields.nonEmpty) {
      _passthroughFields.values.foreach { _.write(_oprot) }
    }
    _oprot.writeFieldStop()
    _oprot.writeStructEnd()
  }

  def copy(
{{#fields}}
    {{fieldName}}: {{>optionalType}} = this.{{fieldName}},
{{/fields}}
    _passthroughFields: immutable$Map[Short, TFieldBlob] = this._passthroughFields
  ): {{StructName}} =
    new {{InstanceClassName}}(
{{#fields}}
      {{fieldName}},
{{/fields}}
      _passthroughFields
    )

  override def canEqual(other: Any): Boolean = other.isInstanceOf[{{StructName}}]

  override def equals(other: Any): Boolean =
    canEqual(other) &&
      _root_.scala.runtime.ScalaRunTime._equals(this, other) &&
      _passthroughFields == other.asInstanceOf[{{StructName}}]._passthroughFields

  override def hashCode: Int = _root_.scala.runtime.ScalaRunTime._hashCode(this)

  override def toString: String = _root_.scala.runtime.ScalaRunTime._toString(this)

{{#hasExceptionMessage}}
  override def getMessage: String = String.valueOf({{exceptionMessageField}})
{{/hasExceptionMessage}}

  override def productArity: Int = {{arity}}

  override def productElement(n: Int): Any = n match {
{{#fields}}
    case {{index}} => this.{{fieldName}}
{{/fields}}
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }

  override def productPrefix: String = "{{StructName}}"
}
