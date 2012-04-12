_oprot.writeMapBegin(new TMap(TType.{{keyType}}, TType.{{valueType}}, _item.size))
_item.foreach { case (_key, _value) =>
  {
    val _item = _key
{{keyWriter}}
  }
  {
    val _item = _value
{{valueWriter}}
  }
}
_oprot.writeMapEnd()
