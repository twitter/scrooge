_oprot.writeSetBegin(new TSet(TType.{{eltType}}, _item.size));
for ({{eltType}} {{eltName}} : _item) {
{{eltWriter}}
}
_oprot.writeSetEnd();