{{#public}}
package {{package}};

import com.twitter.scrooge.ScroogeOption;
import com.twitter.scrooge.Utilities;
import com.twitter.scrooge.ThriftStruct;
import com.twitter.scrooge.ThriftStructCodec;
import com.twitter.util.Function2;
import org.apache.thrift.protocol.*;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
{{#imports}}
import {{parentPackage}}.{{subPackage}}.*;
{{/imports}}

public {{/public}}{{^public}}static {{/public}}class {{name}}{{#isException}} extends Exception{{/isException}} implements ThriftStruct {
  private static final TStruct STRUCT = new TStruct("{{name}}");
{{#fields}}
  private static final TField {{fieldConst}} = new TField("{{name}}", TType.{{constType}}, (short) {{id}});
  final {{#optional}}ScroogeOption<{{fieldType}}>{{/optional}}{{^optional}}{{primitiveFieldType}}{{/optional}} {{name}};
{{/fields}}

  public static class Builder {
{{#fields}}
    private {{primitiveFieldType}} _{{name}} = {{defaultReadValue}};
    private Boolean _got_{{name}} = false;

    public Builder {{name}}({{primitiveFieldType}} value) {
      this._{{name}} = value;
      this._got_{{name}} = true;
      return this;
    }

    public Builder unset{{Name}}() {
      this._{{name}} = {{defaultReadValue}};
      this._got_{{name}} = false;
      return this;
    }
{{/fields}}

    public {{name}} build() {
{{#fields}}
{{#required}}
      if (!_got_{{name}})
      throw new IllegalStateException("Required field '{{name}}' was not found for struct {{struct}}");
{{/required}}
{{/fields}}
      return new {{name}}(
{{#fields}}
{{#optional}}
      ScroogeOption.make(this._got_{{name}}, this._{{name}}){{/optional}}
{{^optional}}
        this._{{name}}{{/optional}}
{{/fields|,
}}    );
    }
  }

  public Builder copy() {
    Builder builder = new Builder();
{{#fields}}
{{#optional}}
    if (this.{{name}}.isDefined()) builder.{{name}}(this.{{name}}.get());
{{/optional}}
{{^optional}}
    builder.{{name}}(this.{{name}});
{{/optional}}
{{/fields}}
    return builder;
  }

  public static ThriftStructCodec<{{name}}> CODEC = new ThriftStructCodec<{{name}}>() {
    public {{name}} decode(TProtocol _iprot) throws org.apache.thrift.TException {
      Builder builder = new Builder();
{{#fields}}
      {{primitiveFieldType}} {{name}} = {{defaultReadValue}};
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
{{#readWriteInfo}}
            {{>readField}}
              builder.{{name}}({{name}});
              break;
{{/readWriteInfo}}
{{/fields}}
            default:
              TProtocolUtil.skip(_iprot, _field.type);
          }
          _iprot.readFieldEnd();
        }
      }
      _iprot.readStructEnd();
      try {
        return builder.build();
      } catch (IllegalStateException stateEx) {
        throw new TProtocolException(stateEx.getMessage());
      }
    }

    public void encode({{name}} struct, TProtocol oprot) throws org.apache.thrift.TException {
      struct.write(oprot);
    }
  };

  public static {{name}} decode(TProtocol _iprot) throws org.apache.thrift.TException {
    return CODEC.decode(_iprot);
  }

  public static void encode({{name}} struct, TProtocol oprot) throws org.apache.thrift.TException {
    CODEC.encode(struct, oprot);
  }

  public {{name}}(
{{#fields}}
    {{#optional}}ScroogeOption<{{fieldType}}>{{/optional}}{{^optional}}{{primitiveFieldType}}{{/optional}} {{name}}
{{/fields|, }}
  ) {
{{#isException}}
{{#fields}}
{{#isMessage}}
    super(message);
{{/isMessage}}
{{/fields}}
{{/isException}}
{{#fields}}
    this.{{name}} = {{name}};
{{/fields}}
  }

{{#alternativeConstructor}}
  public {{name}}(
{{#defaultFields}}
    {{primitiveFieldType}} {{name}}
{{/defaultFields|, }}
  ) {
{{#isException}}
{{#fields}}
{{#isMessage}}
  super(message);
{{/isMessage}}
{{/fields}}
{{/isException}}
{{#fields}}
{{#optional}}
    this.{{name}} = ScroogeOption.none();
{{/optional}}
{{^optional}}
    this.{{name}} = {{name}};
{{/optional}}
{{/fields}}
  }
{{/alternativeConstructor}}

{{#fields}}
{{#hasGetter}}
  public {{primitiveFieldType}} get{{Name}}() {
    return this.{{name}}{{#optional}}.get(){{/optional}};
  }
{{/hasGetter}}
{{#hasIsDefined}}
  public boolean isSet{{Name}}() {
{{#optional}}
    return this.{{name}}.isDefined();
{{/optional}}
{{^optional}}
    return this.{{name}} != null;
{{/optional}}
  }
{{/hasIsDefined}}
{{/fields}}

  public void write(TProtocol _oprot) throws org.apache.thrift.TException {
    validate();
    _oprot.writeStructBegin(STRUCT);
{{#fields}}
{{#readWriteInfo}}
    {{>writeField}}
{{/readWriteInfo}}
{{/fields}}
    _oprot.writeFieldStop();
    _oprot.writeStructEnd();
  }

  private void validate() throws org.apache.thrift.protocol.TProtocolException {
{{#fields}}
{{#required}}
{{#nullable}}
  if (this.{{name}} == null)
      throw new org.apache.thrift.protocol.TProtocolException("Required field '{{name}}' cannot be null");
{{/nullable}}
{{/required}}
{{/fields}}
  }

  public boolean equals(Object other) {
{{#arity0}}
    return this == other;
{{/arity0}}
{{^arity0}}
    if (!(other instanceof {{name}})) return false;
    {{name}} that = ({{name}}) other;
    return
{{#fields}}
{{^isPrimitive}}this.{{name}}.equals(that.{{name}}){{/isPrimitive}}
{{#isPrimitive}}
{{#optional}}
      this.{{name}}.equals(that.{{name}})
{{/optional}}
{{^optional}}
      this.{{name}} == that.{{name}}
{{/optional}}
{{/isPrimitive}}
{{/fields| &&
}};
{{/arity0}}
  }

  public String toString() {
{{#arity0}}
    return "{{name}}()";
{{/arity0}}
{{^arity0}}
    return "{{name}}(" + {{#fields}}this.{{name}}{{/fields| + "," + }} + ")";
{{/arity0}}
  }

  public int hashCode() {
{{#arity0}}
    return super.hashCode();
{{/arity0}}
{{^arity0}}
    int hash = 1;
{{#fields}}
{{#isPrimitive}}
{{#optional}}
    hash = hash * (this.{{name}}.isDefined() ? 0 : new {{fieldType}}(this.{{name}}.get()).hashCode());
{{/optional}}
{{^optional}}
    hash = hash * new {{fieldType}}(this.{{name}}).hashCode();
{{/optional}}
{{/isPrimitive}}
{{^isPrimitive}}
{{#optional}}
    hash = hash * (this.{{name}}.isDefined() ? 0 : this.{{name}}.get().hashCode());
{{/optional}}
{{^optional}}
    hash = hash * (this.{{name}} == null ? 0 : this.{{name}}.hashCode());
{{/optional}}
{{/isPrimitive}}
{{/fields}}
    return hash;
{{/arity0}}
  }
}