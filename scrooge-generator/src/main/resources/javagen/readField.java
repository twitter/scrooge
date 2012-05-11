case {{id}}: /* {{name}} */
  switch (_field.type) {
    case TType.{{constType}}:
      {{fieldType}} {{valueVariableName}};
      {{>readValue}}
      {{name}} = {{valueVariableName}};
      break;
    default:
      TProtocolUtil.skip(_iprot, _field.type);
  }