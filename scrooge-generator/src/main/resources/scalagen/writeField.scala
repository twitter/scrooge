if ({{#optional}}`{{name}}`.isDefined{{/optional}}{{^optional}}true{{/optional}}) {
  val `{{valueVariableName}}` = `{{name}}`{{#optional}}.get{{/optional}}
  _oprot.writeFieldBegin({{fieldConst}})
  {{>writeValue}}
  _oprot.writeFieldEnd()
}
