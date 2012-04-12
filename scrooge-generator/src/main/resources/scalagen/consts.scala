package {{package}}

object Constants {
{{#constants}}
  val {{name}}: {{type}} = {{value}}
{{/constants}}
}
