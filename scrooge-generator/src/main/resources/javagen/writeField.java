{{#optional}}if ({{fieldName}}.isDefined()) {{/optional}}{
  {{fieldType}} {{valueVariableName}} = {{fieldName}}{{#optional}}.get(){{/optional}};
  {{#withSkipNullWrite}}{{^isPrimitive}}if ({{valueVariableName}} != null) {{/isPrimitive}}{{/withSkipNullWrite}}{
    _oprot.writeFieldBegin({{fieldConst}});
    {{>writeValue}}
    _oprot.writeFieldEnd();
  }
}
