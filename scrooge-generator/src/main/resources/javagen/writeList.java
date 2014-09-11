_oprot.writeListBegin(new TList(TType.{{eltConstType}}, {{name}}.size()));
for ({{eltType}} {{eltName}} : {{name}}) {
{{#eltReadWriteInfo}}
  {{>writeValue}}
{{/eltReadWriteInfo}}
}
_oprot.writeListEnd();