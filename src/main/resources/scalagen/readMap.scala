val _map = _iprot.readMapBegin()
val _rv = new mutable.HashMap[{{keyType}}, {{valueType}}]
var _i = 0
while (_i < _map.size) {
  val _key = {
{{keyReader}}
  }
  val _value = {
{{valueReader}}
  }
  _rv(_key) = _value
  _i += 1
}
_iprot.readMapEnd()
_rv
