val _set = _iprot.readSetBegin()
val _rv = new mutable.HashSet[{{eltType}}]
var _i = 0
while (_i < _set.size) {
  _rv += {
{{eltReader}}
  }
  _i += 1
}
_iprot.readSetEnd()
_rv