_field.`type` match {
{{#isEnum}}
  case TType.I32 | TType.ENUM =>
{{/isEnum}}
{{^isEnum}}
  case TType.{{constType}} =>
{{/isEnum}}
    {{fieldName}} = {{#optional}}_root_.scala.Some({{/optional}}{{readFieldValueName}}(_iprot){{#optional}}){{/optional}}
{{#required}}
    {{gotName}} = true
{{/required}}
  case _actualType =>
    val _expectedType = TType.{{#isEnum}}ENUM{{/isEnum}}{{^isEnum}}{{constType}}{{/isEnum}}
    throw new TProtocolException(
      "Received wrong type for field '{{fieldName}}' (expected=%s, actual=%s).".format(
        ttypeToString(_expectedType),
        ttypeToString(_actualType)
      )
    )
}
