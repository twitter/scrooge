// if ({{conditional}}) { FIXME
  {{type}} _item = {{name}};
  _oprot.writeFieldBegin({{fieldConst}});
{{valueWriter}}
  _oprot.writeFieldEnd();
// }