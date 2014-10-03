package {{package}}

@javax.annotation.Generated(value = Array("com.twitter.scrooge.Compiler"))
object {{basename}}Constants {
{{#constants}}
  {{docstring}}
  val {{name}}: {{fieldType}} = {{value}}
{{/constants}}
}
