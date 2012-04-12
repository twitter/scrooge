TList _list = _iprot.readListBegin();
List<{{eltType}}> {{name}} = new ArrayList<{{eltType}}>(_list.size);
int i = 0;
while (_i < _list.size()) {
  {{eltReader}}
  {{name}}.append(element);
  _i += 1;
}
_iprot.readListEnd();
