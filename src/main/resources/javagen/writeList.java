_oprot.writeListBegin(new TList(TType.{{eltType}}, _item.size));
for ({{eltType}} _element : _item) {
  {{eltWriter}}
}
_oprot.writeListEnd();