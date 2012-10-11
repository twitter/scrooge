case {{id}}: /* {{name}} */
  switch (_field.type) {
    case TType.{{constType}}:
      {{fieldType}} {{valueVariableName}};
      {{>readValue}}
      {{fieldName}} = {{valueVariableName}};
      break;
    default:
      TProtocolUtil.skip(_iprot, _field.type);
  }