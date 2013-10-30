_field.`type` match {
{{#isEnum}}
  case TType.I32 | TType.ENUM => {
{{/isEnum}}
{{^isEnum}}
  case TType.{{constType}} => {
{{/isEnum}}
    {{fieldName}} = {{readFieldValueName}}(_iprot)
    {{gotName}} = true
  }
  case _ => TProtocolUtil.skip(_iprot, _field.`type`)
}
