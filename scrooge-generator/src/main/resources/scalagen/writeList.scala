_oprot.writeListBegin(new TList(TType.{{eltWireConstType}}, {{name}}.size))
{{name}}.foreach { {{eltName}} =>
{{#eltReadWriteInfo}}
  {{>writeValue}}
{{/eltReadWriteInfo}}
}
_oprot.writeListEnd()
