package com.twitter.scrooge
package scalagen

import AST._

object StructTemplate extends ScalaTemplate {
  // ----- readers

  val readBasicTemplate = template[FieldType]("_iprot.{{ protocolReadMethod(self) }}()")

  val readBinaryTemplate = template[FieldType](
"""val _buffer = _iprot.readBinary()
val _bytes = new Array[Byte](_buffer.remaining)
_buffer.get(_bytes)
_bytes
""")

  val readListTemplate = template[FieldType](
"""val _list = _iprot.readListBegin()
val _rv = new mutable.ArrayBuffer[{{ scalaType(self.asInstanceOf[AST.ListType].eltType) }}](_list.size)
var _i = 0
while (_i < _list.size) {
  _rv += {
{{ val t = self.asInstanceOf[AST.ListType].eltType; readTemplate(t)(t, scope).indent(2) }}
  }
  _i += 1
}
_iprot.readListEnd()
_rv.toSeq
""")

  val readSetTemplate = template[FieldType](
"""val _set = _iprot.readSetBegin()
val _rv = new mutable.HashSet[{{ scalaType(self.asInstanceOf[AST.SetType].eltType) }}]
var _i = 0
while (_i < _set.size) {
  _rv += {
{{ val t = self.asInstanceOf[AST.SetType].eltType; readTemplate(t)(t, scope).indent(2) }}
  }
  _i += 1
}
_iprot.readSetEnd()
_rv
""")

  val readMapTemplate = template[FieldType](
"""val _map = _iprot.readMapBegin()
val _rv = new mutable.HashMap[{{ val t = self.asInstanceOf[AST.MapType]; scalaType(t.keyType) + ", " + scalaType(t.valueType) }}]
var _i = 0
while (_i < _map.size) {
  val _key = {
{{ val t = self.asInstanceOf[AST.MapType].keyType; readTemplate(t)(t, scope).indent(2) }}
  }
  val _value = {
{{ val t = self.asInstanceOf[AST.MapType].valueType; readTemplate(t)(t, scope).indent(2) }}
  }
  _rv(_key) = _value
  _i += 1
}
_iprot.readMapEnd()
_rv
""")

  val readStructTemplate = template[FieldType](
    """{{self.asInstanceOf[AST.StructType].name}}.decoder(_iprot)""")
  
  val readEnumTemplate = template[FieldType](
    """{{self.asInstanceOf[AST.EnumType].name}}(_iprot.readI32())""")

  def readTemplate(t: FieldType): Template[FieldType] = {
    t match {
      case TBinary => readBinaryTemplate
      case _: ListType => readListTemplate
      case _: SetType => readSetTemplate
      case _: MapType => readMapTemplate
      case _: StructType => readStructTemplate
      case _: EnumType => readEnumTemplate
      case _: BaseType => readBasicTemplate
    }
  }

  val readFieldTemplate = template[Field](
"""case {{id.toString}} => { /* {{name}} */
  _field.`type` match {
    case TType.{{constType(`type`)}} => {
      {{name}} = {{ if (requiredness.isOptional && default == None) "Some" else "" }}{
{{ readTemplate(`type`)(`type`, scope).indent(4) }}
      }
{{
if (requiredness.isRequired) {
  ("_got_" + name + " = true").indent(3)
} else {
  ""
}
}}
    }
    case _ => TProtocolUtil.skip(_iprot, _field.`type`)
  }
}
""")

  // ----- writers

  val writeBasicTemplate = template[FieldType]("""_oprot.{{protocolWriteMethod(self)}}(_item)""")

  val writeBinaryTemplate = template[FieldType]("""_oprot.writeBinary(ByteBuffer.wrap(_item))""")

  val writeListTemplate = template[FieldType](
"""_oprot.writeListBegin(new TList(TType.{{constType(self.asInstanceOf[AST.ListType].eltType)}}, _item.size))
_item.foreach { _item =>
{{ val t = self.asInstanceOf[AST.ListType].eltType; writeTemplate(t)(t, scope).indent(1) }}
}
_oprot.writeListEnd()
""")

  val writeSetTemplate = template[FieldType](
"""_oprot.writeSetBegin(new TSet(TType.{{constType(self.asInstanceOf[AST.SetType].eltType)}}, _item.size))
_item.foreach { _item =>
{{ val t = self.asInstanceOf[AST.SetType].eltType; writeTemplate(t)(t, scope).indent(1) }}
}
_oprot.writeSetEnd()
""")

  val writeMapTemplate = template[FieldType](
"""_oprot.writeMapBegin(new TMap(TType.{{constType(self.asInstanceOf[AST.MapType].keyType)}}, TType.{{constType(self.asInstanceOf[AST.MapType].valueType)}}, _item.size))
_item.foreach { case (_key, _value) =>
  {
    val _item = _key
{{ val t = self.asInstanceOf[AST.MapType].keyType; writeTemplate(t)(t, scope).indent(2) }}
  }
  {
    val _item = _value
{{ val t = self.asInstanceOf[AST.MapType].valueType; writeTemplate(t)(t, scope).indent(2) }}
  }
}
_oprot.writeMapEnd()
""")

  val writeStructTemplate = template[FieldType]("""_item.write(_oprot)""")
  
  val writeEnumTemplate = template[FieldType]("""_oprot.writeI32(_item.value)""")

  def writeTemplate(t: FieldType): Template[FieldType] = {
    t match {
      case TBinary => writeBinaryTemplate
      case _: ListType => writeListTemplate
      case _: SetType => writeSetTemplate
      case _: MapType => writeMapTemplate
      case _: StructType => writeStructTemplate
      case _: EnumType => writeEnumTemplate
      case _: BaseType => writeBasicTemplate
    }
  }

  val writeFieldTemplate = template[Field](
"""if ({{
if (requiredness.isOptional && default == None) {
  name + ".isDefined"
} else {
  `type` match {
    case AST.TBool | AST.TByte | AST.TI16 | AST.TI32 | AST.TI64 | AST.TDouble =>
      "true"
    case _ =>
      name + " ne null"
  }
}
}}) {
  val _item = {{name}}{{if (requiredness.isOptional && default == None) ".get" else ""}}
  _oprot.writeFieldBegin({{ writeFieldConst(name) }})
{{ writeTemplate(`type`)(`type`, scope).indent(1) }}
  _oprot.writeFieldEnd()
}
""")

  // ----- struct

  val structTemplate = template[StructLike](
"""
object {{name}} {
  private val STRUCT_DESC = new TStruct("{{name}}")
{{fields.map { f => "private val " + writeFieldConst(f.name) + " = new TField(\"" + f.name + "\", TType." + constType(f.`type`) + ", " + f.id.toString + ")"}.indent}}

  object decoder extends (TProtocol => {{name}}) {
    override def apply(_iprot: TProtocol) = {
      var _field: TField = null
{{ fields.map { f => "var " + f.name + ": " + scalaFieldType(f) + " = " + defaultValueTemplate(f) }.indent(3) }}
{{ fields.filter { _.requiredness.isRequired }.map { f => "var _got_" + f.name + " = false" }.indent(3) }}
      var _done = false
      _iprot.readStructBegin()
      while (!_done) {
        _field = _iprot.readFieldBegin
        if (_field.`type` == TType.STOP) {
          _done = true
        } else {
          _field.id match {
{{ fields.map { f => readFieldTemplate(f, scope) }.indent(6) }}
            case _ => TProtocolUtil.skip(_iprot, _field.`type`)
          }
          _iprot.readFieldEnd()
        }
      }
      _iprot.readStructEnd()
{{
fields.filter { _.requiredness.isRequired }.map { f =>
  "if (!_got_" + f.name + ") throw new TProtocolException(\"Required field '" + f.name + "' was not found in serialized data for struct " + name + "\")"
}.indent(3)
}}
      {{name}}({{ fields.map { f => f.name }.mkString(", ") }})
    }
  }
}

case class {{name}}({{ fieldArgs(fields) }}) extends {{
  self match {
    case AST.Struct(_, _) => "ThriftStruct"
    case AST.Exception_(_, _) => "Exception with ThriftStruct"
  }
}} {
  import {{name}}._

  override def write(_oprot: TProtocol) {
    validate()

    _oprot.writeStructBegin(STRUCT_DESC)
{{ fields.map { f => writeFieldTemplate(f, scope) }.indent(2) }}
    _oprot.writeFieldStop()
    _oprot.writeStructEnd()
  }

  def validate() = true //TODO: Implement this
}""")
}
