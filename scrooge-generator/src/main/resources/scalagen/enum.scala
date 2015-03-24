package {{package}}

import com.twitter.scrooge.ThriftEnum

{{docstring}}
@javax.annotation.Generated(value = Array("com.twitter.scrooge.Compiler"))
case object {{EnumName}} {
{{#values}}
  {{valuedocstring}}
  case object {{name}} extends {{package}}.{{EnumName}} {
    val value = {{value}}
    val name = "{{name}}"
    val originalName = "{{originalName}}"
  }
{{/values}}

  case class EnumUnknown{{EnumName}}(val value: Int) extends {{package}}.{{EnumName}} {
    val name = "EnumUnknown{{EnumName}}" + value
  }

  /**
   * Find the enum by its integer value, as defined in the Thrift IDL.
   * @throws NoSuchElementException if the value is not found.
   */
  def apply(value: Int): {{package}}.{{EnumName}} =
    value match {
{{#values}}
      case {{value}} => {{package}}.{{EnumName}}.{{name}}
{{/values}}
      case _ => throw new NoSuchElementException(value.toString)
    }

  /**
   * Find the enum by its integer value, as defined in the Thrift IDL.
   * returns an EnumUnknown{{EnumName}}(value) if the value is not found.
   * In particular this allows ignoring new values added to an enum
   * in the IDL on the producer side when the consumer was not updated.
   */
  def getOrUnknown(value: Int): {{package}}.{{EnumName}} =
    get(value) match {
      case scala.Some(e) => e
      case scala.None => EnumUnknown{{EnumName}}(value)
    }

  /**
   * Find the enum by its integer value, as defined in the Thrift IDL.
   * Returns None if the value is not found
   */
  def get(value: Int): Option[{{package}}.{{EnumName}}] =
    value match {
{{#values}}
      case {{value}} => scala.Some({{package}}.{{EnumName}}.{{name}})
{{/values}}
      case _ => scala.None
    }

  def valueOf(name: String): Option[{{package}}.{{EnumName}}] =
    name.toLowerCase match {
{{#values}}
      case "{{unquotedNameLowerCase}}" => scala.Some({{package}}.{{EnumName}}.{{name}})
{{/values}}
      case _ => scala.None
    }

  lazy val list: List[{{package}}.{{EnumName}}] = scala.List[{{package}}.{{EnumName}}](
{{#values}}
    {{package}}.{{EnumName}}.{{name}}
{{/values|,}}
  )
}


{{docstring}}
@javax.annotation.Generated(value = Array("com.twitter.scrooge.Compiler"))
sealed trait {{EnumName}} extends ThriftEnum with Serializable
