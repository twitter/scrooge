object {{enum}} {
{{#values}}
  case object {{name}} extends {{enum}}({{value}})
{{/values}}

  def apply(value: Int): {{enum}} = {
    value match {
      {{#values}}
      case {{value}} => {{name}}
      {{/values}}
      case _ => throw new NoSuchElementException(value.toString)
    }
  }

  def get(value: Int): Option[{{enum}}] = {
    value match {
      {{#values}}
      case {{value}} => scala.Some({{name}})
      {{/values}}
      case _ => scala.None
    }
  }

  def valueOf(name: String): Option[{{enum}}] = {
    name.toLowerCase match {
      {{#values}}
      case "{{nameLowerCase}}" => scala.Some({{enum}}.{{name}})
      {{/values}}
      case _ => scala.None
    }
  }
}

abstract class {{enum}}(val value: Int) extends TEnum {
  def getValue = value
}
