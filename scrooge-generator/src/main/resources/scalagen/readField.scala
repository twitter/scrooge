_field.`type` match {
{{#isEnum}}
  case TType.I32 | TType.ENUM => {
{{/isEnum}}
{{^isEnum}}
  case TType.{{constType}} => {
{{/isEnum}}
    {{fieldName}} = {{#optional}}Some({{/optional}}{{readFieldValueName}}(_iprot){{#optional}}){{/optional}}
{{#required}}
    {{gotName}} = true
{{/required}}
    _readField = true
  }
  case _ => // skip
}
