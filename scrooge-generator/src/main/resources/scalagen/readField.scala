case {{id}} => { /* {{name}} */
  _field.`type` match {
    case TType.{{constType}} => {
      {{fieldName}} = {
        {{>readValue}}
      }
      {{gotName}} = true
    }
    case _ => TProtocolUtil.skip(_iprot, _field.`type`)
  }
}
