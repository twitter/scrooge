{{#public}}
package {{package}}

import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec3}
import org.apache.thrift.protocol._
import java.nio.ByteBuffer
import java.util.Arrays
import scala.collection.mutable.{
  ArrayBuffer => mutable$ArrayBuffer, Buffer => mutable$Buffer,
  HashMap => mutable$HashMap, HashSet => mutable$HashSet}
import scala.collection.{Map, Set}

{{/public}}
@javax.annotation.Generated(value = Array("com.twitter.scrooge.Compiler"), date = "{{date}}")
sealed trait {{StructName}} extends {{parentType}}

{{docstring}}
@javax.annotation.Generated(value = Array("com.twitter.scrooge.Compiler"), date = "{{date}}")
object {{StructName}} extends ThriftStructCodec3[{{StructName}}] {
  val Union = new TStruct("{{StructNameForWire}}")
{{#fields}}
  val {{fieldConst}} = new TField("{{fieldNameForWire}}", TType.{{constType}}, {{id}})
{{#isEnum}}
  private[this] val {{fieldConst}}I32 = new TField("{{fieldNameForWire}}", TType.I32, {{id}})
{{/isEnum}}
{{/fields}}

  override def encode(_item: {{StructName}}, _oprot: TProtocol) { _item.write(_oprot) }
  override def decode(_iprot: TProtocol): {{StructName}} = {
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
