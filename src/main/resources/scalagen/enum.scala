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
      case {{value}} => Some({{name}})
      {{/values}}
      case _ => None
    }
  }
}

abstract class {{enum}}(val value: Int) extends TEnum {
  def getValue = value
}