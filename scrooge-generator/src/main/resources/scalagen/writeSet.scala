_oprot.writeSetBegin(new TSet(TType.{{eltConstType}}, {{name}}.size))
{{name}}.foreach { {{eltName}} =>
{{#eltReadWriteInfo}}
  {{>writeValue}}
{{/eltReadWriteInfo}}
}
_oprot.writeSetEnd()
