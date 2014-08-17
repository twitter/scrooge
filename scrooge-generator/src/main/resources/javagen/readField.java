case {{id}}: /* {{fieldName}} */
  switch (_field.type) {
{{#isEnum}}
    case TType.I32:
    case TType.ENUM:
{{/isEnum}}
{{^isEnum}}
    case TType.{{constType}}:
{{/isEnum}}
      {{fieldType}} {{valueVariableName}};
      {{>readValue}}
      {{fieldName}} = {{valueVariableName}};
      break;
    default:
      TProtocolUtil.skip(_iprot, _field.type);
  }
