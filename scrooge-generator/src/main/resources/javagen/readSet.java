TSet _set_{{name}} = _iprot.readSetBegin();
{{#isEnumSet}}
{{name}} = EnumSet.noneOf({{eltType}}.class);
{{/isEnumSet}}
{{^isEnumSet}}
{{name}} = new HashSet<{{eltType}}>();
{{/isEnumSet}}
int _i_{{name}} = 0;
{{eltType}} {{eltName}};
while (_i_{{name}} < _set_{{name}}.size) {
{{#eltReadWriteInfo}}
  {{>readValue}}
{{/eltReadWriteInfo}}
  {{name}}.add({{eltName}});
  _i_{{name}} += 1;
}
_iprot.readSetEnd();