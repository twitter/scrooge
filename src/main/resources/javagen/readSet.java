TSet _set = _iprot.readSetBegin();
Set<{{eltType}}> {{name}} = new HashSet<{{eltType}}>(_set.size);
int _i = 0;
while (_i < _set.size) {
  {{eltReader}};
  {{name}}.append(element);
  _i += 1;
}
  _iprot.readSetEnd();