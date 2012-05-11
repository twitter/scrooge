case {{id}} => { /* {{name}} */
  _field.`type` match {
    case TType.{{constType}} => {
      `{{name}}` = {
        {{>readValue}}
      }
      _got_{{name}} = true
    }
    case _ => TProtocolUtil.skip(_iprot, _field.`type`)
  }
}
