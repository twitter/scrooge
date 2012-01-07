case {{id}} => { /* {{name}} */
  _field.`type` match {
    case TType.{{constType}} => {
      `{{name}}` = {{optionality}}{
{{valueReader}}
      }
{{#required}}
      _got_{{name}} = true
{{/required}}
    }
    case _ => TProtocolUtil.skip(_iprot, _field.`type`)
  }
}
