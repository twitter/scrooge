TMap _map = _iprot.readMapBegin();
Map<{{keyType}}, {{valueType}}> {{name}} = new HashMap<{{keyType}}, {{valueType}}>();
int _i = 0;
while (_i < _map.size()) {
  {{keyReader}}
  {{valueReader}}
  {{name}}.update(key, value);
  _i += 1;
}
_iprot.readMapEnd();