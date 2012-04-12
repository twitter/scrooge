_oprot.writeListBegin(new TList(TType.{{eltType}}, {{name}}.size()));
for ({{eltType}} _element : {{name}}) {
  {{eltWriter}}
}
_oprot.writeListEnd();