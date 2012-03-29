_oprot.writeMapBegin(new TMap(TType.{{keyType}}, TType.{{valueType}}, _item.size));
for (Map.Entry<{{keyType}}, {{valueType}}> entry : _item.entrySet()) {
  {{keyType}} {{keyName}} = entry.getKey();
  {{keyWriter}}
  {{valueType}} {{valueName}} = entry.getValue();
  {{valueWriter}}
}
_oprot.writeMapEnd();