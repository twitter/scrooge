_oprot.writeSetBegin(new TSet(TType.{{eltType}}, _item.size))
_item.foreach { _item =>
{{eltWriter}}
}
_oprot.writeSetEnd()
