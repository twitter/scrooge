TMap _map_{{name}} = _iprot.readMapBegin();
{{name}} = new HashMap<{{keyType}}, {{valueType}}>();
int _i_{{name}} = 0;
{{#keyReadWriteInfo}}
  {{>qualifiedFieldType}} {{keyName}};
{{/keyReadWriteInfo}}
{{#valueReadWriteInfo}}
  {{>qualifiedFieldType}} {{valueName}};
{{/valueReadWriteInfo}}

while (_i_{{name}} < _map_{{name}}.size) {
{{#keyReadWriteInfo}}
  {{>readValue}}
{{/keyReadWriteInfo}}
{{#valueReadWriteInfo}}
  {{>readValue}}
{{/valueReadWriteInfo}}
  {{name}}.put({{keyName}}, {{valueName}});
  _i_{{name}} += 1;
}
_iprot.readMapEnd();
