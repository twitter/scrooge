package {{package}}

import org.apache.thrift.TEnum

object {{enum_name}} {
{{#values}}
  case object {{name}} extends {{enum_name}}({{value}}, "{{name}}")
{{/values}}

  def apply(value: Int): {{enum_name}} = {
    value match {
{{#values}}
      case {{value}} => {{name}}
{{/values}}
      case _ => throw new NoSuchElementException(value.toString)
    }
  }

  def get(value: Int): Option[{{enum_name}}] = {
    value match {
{{#values}}
      case {{value}} => scala.Some({{name}})
{{/values}}
      case _ => scala.None
    }
  }

  def valueOf(name: String): Option[{{enum_name}}] = {
    name.toLowerCase match {
{{#values}}
      case "{{nameLowerCase}}" => scala.Some({{enum_name}}.{{name}})
{{/values}}
      case _ => scala.None
    }
  }
}

abstract class {{enum_name}}(val value: Int, val name: String) extends TEnum {
  def getValue = value
}

