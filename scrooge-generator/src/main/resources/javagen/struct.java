{{#public}}
package {{package}};

import com.twitter.scrooge.Utilities;
import org.apache.thrift.protocol.*;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public {{/public}}{{^public}}static {{/public}}class {{name}} {
  private static final TStruct STRUCT = new TStruct("{{name}}");
{{#fields}}
  private static final TField {{fieldConst}} = new TField("{{name}}", TType.{{constType}}, (short) {{id}});
  private final {{fieldType}} {{name}};
{{/fields}}

  private static {{name}} decode(TProtocol _iprot) throws org.apache.thrift.TException{
{{#fields}}
    {{fieldType}} {{name}} = {{defaultReadValue}};
{{/fields}}
{{#fields}}
{{#required}}
    Boolean _got_{{name}} = false;
{{/required}}
{{/fields}}
    Boolean _done = false;
    _iprot.readStructBegin();
    while (!_done) {
      TField _field = _iprot.readFieldBegin();
      if (_field.type == TType.STOP) {
        _done = true;
      } else {
        switch (_field.id) {
{{#fields}}
          {{reader}}
{{/fields}}
          default:
            TProtocolUtil.skip(_iprot, _field.type);
        }
        _iprot.readFieldEnd();
      }
    }
    _iprot.readStructEnd();
{{#fields}}
{{#required}}
    if (!_got_{{name}})
      throw new TProtocolException("Required field '{{name}}' was not found in serialized data for struct {{struct}}");
{{/required}}
{{/fields}}
    return new {{name}}({{#fields}}{{name}}{{/fields|, }});
}

  private static void encode({{name}} item, TProtocol oproto) throws org.apache.thrift.TException {
    item.write(oproto);
  }

  public {{name}}(
{{#fields}}
  {{fieldType}} {{name}}{{comma}}
{{/fields}}
  ) {
{{#fields}}
    this.{{name}} = {{name}};
{{/fields}}
  }

{{#fields}}
  {{fieldType}} get{{name}}() {
    return this.{{name}};
  }
{{/fields}}

  void write(TProtocol _oprot) throws org.apache.thrift.TException {
    validate();
    _oprot.writeStructBegin(STRUCT);
{{#fields}}
    {{writer}}
{{/fields}}
    _oprot.writeFieldStop();
    _oprot.writeStructEnd();
  }

  void validate() {
{{#fields}}
{{#required}}
{{#nullable}}
    if (this.{{name}} == null)
      throw new TProtocolException("Required field '{{name}}' cannot be null");
{{/nullable}}
{{/required}}
{{/fields}}
  }

}