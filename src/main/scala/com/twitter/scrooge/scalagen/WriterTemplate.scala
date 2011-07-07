package com.twitter.scrooge
package scalagen

import AST._

object WriterTemplate extends ScalaTemplate {
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
