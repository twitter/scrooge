_oprot.writeListBegin(new TList(TType.{{eltConstType}}, `{{name}}`.size))
`{{name}}`.foreach { `{{eltName}}` =>
{{#eltReadWriteInfo}}
  {{>writeValue}}
{{/eltReadWriteInfo}}
}
_oprot.writeListEnd()
