package {{package}}

object Constants {
{{#constants}}
  {{docstring}}
  val {{name}}: {{type}} = {{value}}
{{/constants}}
}
