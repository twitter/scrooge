{{#optional}}if ({{fieldName}}.isDefined()) {{{/optional}}
  _oprot.writeFieldBegin({{fieldConst}}{{#isEnum}}I32{{/isEnum}});
  {{>qualifiedFieldType}} {{valueVariableName}} = {{fieldName}}{{#optional}}.get(){{/optional}};
  {{>writeValue}}
  _oprot.writeFieldEnd();
{{#optional}}}{{/optional}}
