package {{package}}

object Constants {
{{#constants}}
  {{docstring}}
  val {{name}}: {{fieldType}} = {{value}}
{{/constants}}
}
