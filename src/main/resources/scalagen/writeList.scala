_oprot.writeListBegin(new TList(TType.{{eltType}}, _item.size))
_item.foreach { _item =>
{{eltWriter}}
}
_oprot.writeListEnd()