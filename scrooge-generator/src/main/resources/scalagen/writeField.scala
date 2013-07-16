{{#optional}}
if ({{fieldName}}.isDefined) {
{{/optional}}
{{^optional}}
{{#nullable}}
if ({{fieldName}} ne null) {
{{/nullable}}
{{^nullable}}
if (true) {
{{/nullable}}
{{/optional}}
  val {{valueVariableName}} = {{fieldName}}{{#optional}}.get{{/optional}}
  _oprot.writeFieldBegin({{fieldConst}})
  {{>writeValue}}
  _oprot.writeFieldEnd()
}
