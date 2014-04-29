_typ match {
{{#isEnum}}
  case TType.I32 | TType.ENUM => {
{{/isEnum}}
{{^isEnum}}
  case TType.{{constType}} => {
{{/isEnum}}
    {{#optional}}Some({{/optional}}{{readFieldValueName}}(_iprot){{#optional}}){{/optional}}
  }
  case _actualType =>
    val _expectedType = TType.{{#isEnum}}ENUM{{/isEnum}}{{^isEnum}}{{constType}}{{/isEnum}}

    throw new TProtocolException(
      "Received wrong type for field '{{fieldName}}' (expected=%s, actual=%s).".format(
        ttypeToHuman(_expectedType),
        ttypeToHuman(_actualType)
      )
    )
}
