{{#optional}}if ({{fieldName}}.isDefined()) {{{/optional}}
  _oprot.writeFieldBegin({{fieldConst}});
  {{>qualifiedFieldType}} {{valueVariableName}} = {{fieldName}}{{#optional}}.get(){{/optional}};
  {{>writeValue}}
  _oprot.writeFieldEnd();
{{#optional}}}{{/optional}}
