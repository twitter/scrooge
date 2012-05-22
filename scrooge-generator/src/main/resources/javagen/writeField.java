{{#optional}}if ({{name}}.isDefined()) {{{/optional}}
  _oprot.writeFieldBegin({{fieldConst}});
  {{fieldType}} {{valueVariableName}} = {{name}}{{#optional}}.get(){{/optional}};
  {{>writeValue}}
  _oprot.writeFieldEnd();
{{#optional}}}{{/optional}}
