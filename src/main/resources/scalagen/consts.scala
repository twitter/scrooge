{{#hasConstants}}
// ----- constants

object Constants {
  {{#constants}}
  val {{name}}: {{type}} = {{value}}
  {{/constants}}
}
{{/hasConstants}}
