{{#optional}}if (`{{name}}`.isDefined) {{{/optional}}
  val `{{valueVariableName}}` = `{{name}}`{{#optional}}.get{{/optional}}

  _oprot.writeFieldBegin({{fieldConst}})
  {{>writeValue}}
  _oprot.writeFieldEnd()
{{#optional}}}{{/optional}}
