case {{id}}: /* {{name}} */
  switch (_field.type) {
    case TType.{{constType}}:
      {{valueReader}}
{{#required}}
      _got_{{name}} = true;
{{/required}}

    default:
      TProtocolUtil.skip(_iprot, _field.type);
  }