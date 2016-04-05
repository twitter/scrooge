package {{package}}

import com.twitter.scrooge.ThriftEnum

{{docstring}}
@javax.annotation.Generated(value = Array("com.twitter.scrooge.Compiler"))
case object {{EnumName}} {
{{#values}}
  {{valuedocstring}}
  case object {{name}} extends {{package}}.{{EnumName}} {
    val value: Int = {{value}}
    val name: String = "{{name}}"
    val originalName: String = "{{originalName}}"
  }

  private[this] val _Some{{name}} = _root_.scala.Some({{package}}.{{EnumName}}.{{name}})
{{/values}}

  case class EnumUnknown{{EnumName}}(value: Int) extends {{package}}.{{EnumName}} {
    val name: String = "EnumUnknown{{EnumName}}" + value
    def originalName: String = name
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
      case _root_.scala.Some(e) => e
      case _root_.scala.None => EnumUnknown{{EnumName}}(value)
    }

  /**
   * Find the enum by its integer value, as defined in the Thrift IDL.
   * Returns None if the value is not found
   */
  def get(value: Int): _root_.scala.Option[{{package}}.{{EnumName}}] =
    value match {
{{#values}}
      case {{value}} => _Some{{name}}
{{/values}}
      case _ => _root_.scala.None
    }

  def valueOf(name: String): _root_.scala.Option[{{package}}.{{EnumName}}] =
    name.toLowerCase match {
{{#values}}
      case "{{unquotedNameLowerCase}}" => _Some{{name}}
{{/values}}
      case _ => _root_.scala.None
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
