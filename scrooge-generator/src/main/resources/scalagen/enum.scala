package {{package}}

import org.apache.thrift.TEnum

{{docstring}}
object {{enum_name}} {
{{#values}}
  case object {{name}} extends {{enum_name}}({{value}}, "{{name}}")
{{/values}}

  /**
   * Find the enum by its integer value, as defined in the Thrift IDL.
   * @throws NoSuchElementException if the value is not found.
   */
  def apply(value: Int): {{enum_name}} = {
    value match {
{{#values}}
      case {{value}} => {{name}}
{{/values}}
      case _ => throw new NoSuchElementException(value.toString)
    }
  }

  /**
   * Find the enum by its integer value, as defined in the Thrift IDL.
   * Returns None if the value is not found
   */
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

{{docstring}}
abstract class {{enum_name}}(val value: Int, val name: String) extends TEnum {
  def getValue = value
}

