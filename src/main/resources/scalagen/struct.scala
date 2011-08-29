object {{name}} extends ThriftStructCodec[{{name}}] {
  val STRUCT_DESC = new TStruct("{{name}}")
  {{#fields}}
  val {{fieldConst}} = new TField("{{name}}", TType.{{constType}}, {{id}})
  {{/fields}}

  val decoder = { (_iprot: TProtocol) =>
    var _field: TField = null
    {{#fields}}
    var {{name}}: {{scalaType}} = {{defaultReadValue}}
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

  val encoder = { (_item: {{name}}, _oproto: TProtocol) => _item.write(_oproto) }
}

case class {{name}}({{fieldArgs}}) extends {{parentType}} {
  import {{name}}._

  {{#optionalDefaults}}
  def {{name}}OrDefault = {{name}} getOrElse {{value}}
  {{/optionalDefaults}}

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
