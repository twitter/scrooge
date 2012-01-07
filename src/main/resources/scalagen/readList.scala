val _list = _iprot.readListBegin()
val _rv = new mutable.ArrayBuffer[{{eltType}}](_list.size)
var _i = 0
while (_i < _list.size) {
  _rv += {
{{eltReader}}
  }
  _i += 1
}
_iprot.readListEnd()
_rv.toSeq
