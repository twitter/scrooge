TList _list_{{name}} = _iprot.readListBegin();
{{name}} = new ArrayList<{{eltType}}>();
int _i_{{name}} = 0;
{{eltType}} {{eltName}};
while (_i_{{name}} < _list_{{name}}.size) {
{{#eltReadWriteInfo}}
  {{>readValue}}
{{/eltReadWriteInfo}}
  {{name}}.add({{eltName}});
  _i_{{name}} += 1;
}
_iprot.readListEnd();
