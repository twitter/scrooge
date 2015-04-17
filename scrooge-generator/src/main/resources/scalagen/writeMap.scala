_oprot.writeMapBegin(new TMap(TType.{{keyWireConstType}}, TType.{{valueWireConstType}}, {{name}}.size))
{{name}}.foreach { case ({{keyName}}, {{valueName}}) =>
{{#keyReadWriteInfo}}
  {{>writeValue}}
{{/keyReadWriteInfo}}
{{#valueReadWriteInfo}}
  {{>writeValue}}
{{/valueReadWriteInfo}}
}
_oprot.writeMapEnd()
