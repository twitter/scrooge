_oprot.writeMapBegin(new TMap(TType.{{keyConstType}}, TType.{{valueConstType}}, `{{name}}`.size))
`{{name}}`.foreach { _pair =>
  val `{{keyName}}` = _pair._1
  val `{{valueName}}` = _pair._2
{{#keyReadWriteInfo}}
  {{>writeValue}}
{{/keyReadWriteInfo}}
{{#valueReadWriteInfo}}
  {{>writeValue}}
{{/valueReadWriteInfo}}
}
_oprot.writeMapEnd()
