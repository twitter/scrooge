{{#public}}
package {{package}}

import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec}
import org.apache.thrift.protocol._
import java.nio.ByteBuffer
{{#finagle}}
import com.twitter.finagle.SourcedException
{{/finagle}}
import scala.collection.mutable
import scala.collection.{Map, Set}
{{#imports}}
import {{parentpackage}}.{{{subpackage}} => {{_alias_}}}
{{/imports}}

{{/public}}
sealed trait {{StructName}} extends {{parentType}}

{{docstring}}
object {{StructName}} extends ThriftStructCodec[{{StructName}}] {
  val Union = new TStruct("{{StructName}}")
{{#fields}}
  val {{fieldConst}} = new TField("{{fieldName}}", TType.{{constType}}, {{id}})
{{/fields}}

  def encode(_item: {{StructName}}, _oprot: TProtocol) { _item.write(_oprot) }
  def decode(_iprot: TProtocol): {{StructName}} = {
    var _result: {{StructName}} = null
    _iprot.readStructBegin()
    val _field = _iprot.readFieldBegin()
    _field.id match {
{{#fields}}
{{#readWriteInfo}}
      {{>readUnionField}}
{{/readWriteInfo}}
{{/fields}}
      case _ => TProtocolUtil.skip(_iprot, _field.`type`)
    }
    if (_field.`type` != TType.STOP) {
      _iprot.readFieldEnd()
      var _done = false
      var _moreThanOne = false
      while (!_done) {
        val _field = _iprot.readFieldBegin()
        if (_field.`type` == TType.STOP)
          _done = true
        else {
          _moreThanOne = true
          TProtocolUtil.skip(_iprot, _field.`type`)
          _iprot.readFieldEnd()
        }
      }
      if (_moreThanOne) {
        _iprot.readStructEnd()
        throw new TProtocolException("Cannot read a TUnion with more than one set value!")
      }
    }
    _iprot.readStructEnd()
    if (_result == null)
      throw new TProtocolException("Cannot read a TUnion with no set value!")
    _result
  }

  def apply(_iprot: TProtocol): {{StructName}} = decode(_iprot)

{{#fields}}
  case class {{FieldName}}({{fieldName}}: {{>qualifiedFieldType}}{{#hasDefaultValue}} = {{defaultFieldValue}}{{/hasDefaultValue}}) extends {{StructName}} {
    override def write(_oprot: TProtocol) {
      if ({{fieldName}} == null)
        throw new TProtocolException("Cannot write a TUnion with no set value!")
      _oprot.writeStructBegin(Union)
{{#readWriteInfo}}
      {{>writeField}}
{{/readWriteInfo}}
      _oprot.writeFieldStop()
      _oprot.writeStructEnd()
    }
  }
{{/fields}}
}
