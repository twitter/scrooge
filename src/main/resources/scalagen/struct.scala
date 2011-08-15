object {{name}} {
  private val STRUCT_DESC = new TStruct("{{name}}")
  {{#fields}}
  private val {{fieldConst}} = new TField("{{name}}", TType.{{constType}}, {{id}})
  {{/fields}}

  object decoder extends (TProtocol => {{name}}) {
    override def apply(_iprot: TProtocol) = {
      var _field: TField = null
      {{#fields}}
      var {{name}}: {{scalaType}} = {{defaultValue}}
      {{#required}}var _got_{{name}} = false{{/required}}
      {{/fields}}
      var _done = false
      _iprot.readStructBegin()
      while (!_done) {
        _field = _iprot.readFieldBegin
        if (_field.`type` == TType.STOP) {
          _done = true
        } else {
          _field.id match {
          {{#fields}}
{{reader}}
          {{/fields}}
            case _ => TProtocolUtil.skip(_iprot, _field.`type`)
          }
          _iprot.readFieldEnd()
        }
      }
      _iprot.readStructEnd()
      {{#fields}}
      {{#required}}
      if (!_got_{{name}}) throw new TProtocolException("Required field '{{name}}' was not found in serialized data for struct {{struct}}")
      {{/required}}
      {{/fields}}
      {{name}}({{fieldNames}})
    }
  }
}

case class {{name}}({{fieldArgs}}) extends {{parentType}} {
  import {{name}}._

  override def write(_oprot: TProtocol) {
    validate()
    _oprot.writeStructBegin(STRUCT_DESC)
    {{#fields}}
{{writer}}
    {{/fields}}
    _oprot.writeFieldStop()
    _oprot.writeStructEnd()
  }

  def validate() = true //TODO: Implement this
}