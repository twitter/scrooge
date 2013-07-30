_oprot.writeSetBegin(new TSet(TType.{{eltConstType}}, {{name}}.size()));
for ({{eltType}} {{eltName}} : {{name}}) {
{{#eltReadWriteInfo}}
  {{>writeValue}}
{{/eltReadWriteInfo}}
}
_oprot.writeSetEnd();