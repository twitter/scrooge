_oprot.writeMapBegin(new TMap(TType.{{keyWireConstType}}, TType.{{valueWireConstType}}, {{name}}.size))
{{name}}.foreach { _pair =>
  val {{keyName}} = _pair._1
  val {{valueName}} = _pair._2
{{#keyReadWriteInfo}}
  {{>writeValue}}
{{/keyReadWriteInfo}}
{{#valueReadWriteInfo}}
  {{>writeValue}}
{{/valueReadWriteInfo}}
}
_oprot.writeMapEnd()
