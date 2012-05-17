{{#public}}
package {{package}}

import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec}
import org.apache.thrift.protocol._
import java.nio.ByteBuffer
import com.twitter.finagle.SourcedException
import scala.collection.mutable
import scala.collection.{Map, Set}
{{#imports}}
import {{parentPackage}}.{{{subPackage}} => _{{alias}}_}
{{/imports}}

{{/public}}
object {{name}} extends ThriftStructCodec[{{name}}] {
  val Struct = new TStruct("{{name}}")
{{#fields}}
  val {{fieldConst}} = new TField("{{name}}", TType.{{constType}}, {{id}})
{{/fields}}

  def encode(_item: {{name}}, _oproto: TProtocol) { _item.write(_oproto) }
  def decode(iprot: TProtocol) = Immutable.decode(iprot)

  def apply(_iprot: TProtocol): {{name}} = decode(_iprot)

  def apply(
{{#fields}}
    `{{name}}`: {{>optionalType}}{{#hasDefaultValue}} = {{defaultFieldValue}}{{/hasDefaultValue}}{{#optional}} = None{{/optional}}
{{/fields|,}}
  ): {{name}} = new Immutable(
{{#fields}}
    `{{name}}`
{{/fields|,}}
  )

{{#arity0}}
  def unapply(_item: {{name}}): Boolean = true
{{/arity0}}
{{#arity1}}
  def unapply(_item: {{struct}}): Option[{{>optionalType}}] = Some(_item.{{name}})
{{/arity1}}
{{#arityN}}
  def unapply(_item: {{name}}): Option[{{product}}] = Some(_item)
{{/arityN}}

  object Immutable extends ThriftStructCodec[{{name}}] {
    def encode(_item: {{name}}, _oproto: TProtocol) { _item.write(_oproto) }
    def decode(_iprot: TProtocol) = {
{{#fields}}
      var `{{name}}`: {{fieldType}} = {{defaultReadValue}}
      var _got_{{name}} = false
{{/fields}}
      var _done = false
      _iprot.readStructBegin()
      while (!_done) {
        val _field = _iprot.readFieldBegin()
        if (_field.`type` == TType.STOP) {
          _done = true
        } else {
          _field.id match {
{{#fields}}
{{#readWriteInfo}}
            {{>readField}}
{{/readWriteInfo}}
{{/fields}}
            case _ => TProtocolUtil.skip(_iprot, _field.`type`)
          }
          _iprot.readFieldEnd()
        }
      }
      _iprot.readStructEnd()
{{#fields}}
{{#required}}
      if (!_got_{{name}}) throw new TProtocolException("Required field '{{name}}' was not found in serialized data for struct {{struct}}")
{{/required}}
{{/fields}}
      new Immutable(
{{#fields}}
{{#optional}}
        if (_got_{{name}}) Some(`{{name}}`) else None
{{/optional}}
{{^optional}}
        `{{name}}`
{{/optional}}
{{/fields|,}}
      )
    }
  }

  /**
   * The default read-only implementation of {{name}}.  You typically should not need to
   * directly reference this class; instead, use the {{name}}.apply method to construct
   * new instances.
   */
  class Immutable(
{{#fields}}
    val `{{name}}`: {{>optionalType}}{{#hasDefaultValue}} = {{defaultFieldValue}}{{/hasDefaultValue}}{{#optional}} = None{{/optional}}
{{/fields|,}}
  ) extends {{name}}

{{#withProxy}}
  /**
   * This Proxy trait allows you to extend the {{name}} trait with additional state or
   * behavior and implement the read-only methods from {{name}} using an underlying
   * instance.
   */
  trait Proxy extends {{name}} {
    protected def _underlying{{name}}: {{name}}
{{#fields}}
    def `{{name}}`: {{>optionalType}} = _underlying{{struct}}.`{{name}}`
{{/fields}}
  }
{{/withProxy}}
}

trait {{name}} extends {{parentType}}
  with {{product}}
  with java.io.Serializable
{
  import {{name}}._

{{#fields}}
  def `{{name}}`: {{>optionalType}}
{{/fields}}

{{#fields}}
  def _{{indexP1}} = `{{name}}`
{{/fields}}

  override def write(_oprot: TProtocol) {
    validate()
    _oprot.writeStructBegin(Struct)
{{#fields}}
{{#readWriteInfo}}
    {{>writeField}}
{{/readWriteInfo}}
{{/fields}}
    _oprot.writeFieldStop()
    _oprot.writeStructEnd()
  }

  def copy(
{{#fields}}
    `{{name}}`: {{>optionalType}} = this.`{{name}}`{{comma}}
{{/fields}}
  ): {{name}} = new Immutable(
{{#fields}}
    `{{name}}`{{comma}}
{{/fields}}
  )

  /**
   * Checks that all required fields are non-null.
   */
  def validate() {
{{#fields}}
{{#required}}
{{#nullable}}
    if (`{{name}}` == null) throw new TProtocolException("Required field '{{name}}' cannot be null")
{{/nullable}}
{{/required}}
{{/fields}}
  }

  def canEqual(other: Any) = other.isInstanceOf[{{name}}]

  override def equals(other: Any): Boolean = runtime.ScalaRunTime._equals(this, other)

  override def hashCode: Int = runtime.ScalaRunTime._hashCode(this)

  override def toString: String = runtime.ScalaRunTime._toString(this)

  override def productArity = {{arity}}

  override def productElement(n: Int): Any = n match {
{{#fields}}
    case {{index}} => `{{name}}`
{{/fields}}
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }

  override def productPrefix = "{{name}}"
}
