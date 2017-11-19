_field.`type` match {
{{#isEnum}}
  case TType.I32 | TType.ENUM =>
{{/isEnum}}
{{^isEnum}}
  case TType.{{constType}} =>
{{/isEnum}}
    AdaptTProtocol.usedStartMarker({{id}})
    {{fieldName}} = {{#optional}}_root_.scala.Some({{/optional}}{{StructName}}.{{readFieldValueName}}(_iprot){{#optional}}){{/optional}}
    AdaptTProtocol.usedEndMarker({{id}})
    AdaptTProtocol.unusedStartMarker({{id}})
{{#readWriteInfo}}
    {{>skipValue}}
{{/readWriteInfo}}
    AdaptTProtocol.unusedEndMarker({{id}})
{{#required}}
    {{gotName}} = true
{{/required}}
  case _actualType =>
    val _expectedType = TType.{{#isEnum}}ENUM{{/isEnum}}{{^isEnum}}{{constType}}{{/isEnum}}
    throw AdaptTProtocol.unexpectedTypeException(_expectedType, _actualType, "{{fieldName}}")
}
AdaptTProtocol.usedStartMarker({{id}})
adapt.{{setName}}({{fieldName}})
AdaptTProtocol.usedEndMarker({{id}})
