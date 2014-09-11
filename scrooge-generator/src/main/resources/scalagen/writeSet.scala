_oprot.writeSetBegin(new TSet(TType.{{eltWireConstType}}, {{name}}.size))
{{name}}.foreach { {{eltName}} =>
{{#eltReadWriteInfo}}
  {{>writeValue}}
{{/eltReadWriteInfo}}
}
_oprot.writeSetEnd()
