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
val _rv = new mutable.ListBuffer[{{ scalaType(self.asInstanceOf[AST.ListType].tpe) }}]
var _i = 0
while (_i < _list.size) {
  _rv += {
{{ val t = self.asInstanceOf[AST.ListType].tpe; readTemplate(t)(t, scope).indent(2) }}
  }
  _i += 1
}
_iprot.readListEnd()
_rv.toList
""")

  val readSetTemplate = template[FieldType](
"""val _set = _iprot.readSetBegin()
val _rv = new mutable.HashSet[{{ scalaType(self.asInstanceOf[AST.SetType].tpe) }}]
var _i = 0
while (_i < _set.size) {
  _rv += {
{{ val t = self.asInstanceOf[AST.SetType].tpe; readTemplate(t)(t, scope).indent(2) }}
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

  val readStructTemplate = template[FieldType]("""{{self.asInstanceOf[AST.ReferenceType].name}}.decoder(_iprot)""")

  def readTemplate(t: FieldType): Template[FieldType] = {
    t match {
      case TBinary => readBinaryTemplate
      case _: ListType => readListTemplate
      case _: SetType => readSetTemplate
      case _: MapType => readMapTemplate
      case _: ReferenceType => readStructTemplate
      case _ => readBasicTemplate
    }
  }

  val readFieldTemplate = template[Field](
"""case {{id.toString}} => { /* {{name}} */
  _field.`type` match {
    case TType.{{constType(`type`)}} => {
      {{name}} = {{ if (requiredness == AST.Requiredness.Optional && default == None) "Some" else "" }}{
{{ readTemplate(`type`)(`type`, scope).indent(4) }}
      }
{{
if (requiredness == AST.Requiredness.Required) {
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

  val writeBasicTemplate = template[FieldType]("""oprot.{{protocolWriteMethod(self)}}(_item)""")

  val writeBinaryTemplate = template[FieldType]("""oprot.writeBinary(ByteBuffer.wrap(_item))""")

  val writeListTemplate = template[FieldType](
"""oprot.writeListBegin(new TList(TType.{{constType(self.asInstanceOf[AST.ListType].tpe)}}, _item.size))
_item.foreach { _item =>
{{ val t = self.asInstanceOf[AST.ListType].tpe; writeTemplate(t)(t, scope).indent(1) }}
}
oprot.writeListEnd()
""")

  val writeSetTemplate = template[FieldType](
"""oprot.writeSetBegin(new TSet(TType.{{constType(self.asInstanceOf[AST.SetType].tpe)}}, _item.size))
_item.foreach { _item =>
{{ val t = self.asInstanceOf[AST.SetType].tpe; writeTemplate(t)(t, scope).indent(1) }}
}
oprot.writeSetEnd()
""")

  val writeMapTemplate = template[FieldType](
"""oprot.writeMapBegin(new TMap(TType.{{constType(self.asInstanceOf[AST.MapType].keyType)}}, TType.{{constType(self.asInstanceOf[AST.MapType].valueType)}}, _item.size))
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
oprot.writeMapEnd()
""")

  val writeStructTemplate = template[FieldType]("""_item.write(oprot)""")

  def writeTemplate(t: FieldType): Template[FieldType] = {
    t match {
      case TBinary => writeBinaryTemplate
      case _: ListType => writeListTemplate
      case _: SetType => writeSetTemplate
      case _: MapType => writeMapTemplate
      case _: ReferenceType => writeStructTemplate
      case _ => writeBasicTemplate
    }
  }

  val writeFieldTemplate = template[Field](
"""if ({{
if (requiredness == AST.Requiredness.Optional && default == None) {
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
  val _item = {{name}}{{if (requiredness == AST.Requiredness.Optional && default == None) ".get" else ""}}
  oprot.writeFieldBegin({{ writeFieldConst(name) }})
{{ writeTemplate(`type`)(`type`, scope).indent(1) }}
  oprot.writeFieldEnd()
}
""")
}
