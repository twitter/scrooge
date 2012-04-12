_oprot.writeSetBegin(new TSet(TType.{{eltType}}, {{name}}.size()));
for ({{eltType}} {{eltName}} : {{name}}) {
  {{eltWriter}}
}
_oprot.writeSetEnd();