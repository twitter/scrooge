if ({{conditional}}) {
  val _item = `{{name}}`{{getter}}
  _oprot.writeFieldBegin({{fieldConst}})
{{valueWriter}}
  _oprot.writeFieldEnd()
}
