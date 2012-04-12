_oprot.writeMapBegin(new TMap(TType.{{keyType}}, TType.{{valueType}}, {{name}}.size()));
for (Map.Entry<{{keyType}}, {{valueType}}> entry : {{name}}.entrySet()) {
  {{keyType}} {{keyName}} = entry.getKey();
  {{keyWriter}}
  {{valueType}} {{valueName}} = entry.getValue();
  {{valueWriter}}
}
_oprot.writeMapEnd();