{{#public}}

package {{package}};

import com.twitter.scrooge.Option;
import com.twitter.scrooge.Utilities;
import com.twitter.scrooge.ThriftStruct;
import com.twitter.scrooge.ThriftStructCodec;
import com.twitter.scrooge.ThriftStructCodec3;
import org.apache.thrift.protocol.*;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

{{docstring}}
@javax.annotation.Generated(value = "com.twitter.scrooge.Compiler", date = "{{date}}")
public {{/public}}{{^public}}static {{/public}}class {{StructName}}{{#isException}} extends Exception{{/isException}} implements ThriftStruct {
  private static final TStruct STRUCT = new TStruct("{{StructNameForWire}}");
{{#fields}}
  private static final TField {{fieldConst}} = new TField("{{fieldNameForWire}}", TType.{{constType}}, (short) {{id}});
  final {{#optional}}Option<{{fieldType}}>{{/optional}}{{^optional}}{{primitiveFieldType}}{{/optional}} {{fieldName}};
{{/fields}}

  public static class Builder {
{{#fields}}
    private {{primitiveFieldType}} {{_fieldName}} = {{defaultReadValue}};
    private Boolean {{gotName}} = false;

    public Builder {{fieldName}}({{primitiveFieldType}} value) {
      this.{{_fieldName}} = value;
      this.{{gotName}} = true;
      return this;
    }

    public Builder {{unsetName}}() {
      this.{{_fieldName}} = {{defaultReadValue}};
      this.{{gotName}} = false;
      return this;
    }
{{/fields}}

    public {{StructName}} build() {
{{#fields}}
{{#required}}
      if (!{{gotName}})
      throw new IllegalStateException("Required field '{{fieldName}}' was not found for struct {{struct}}");
{{/required}}
{{/fields}}
      return new {{StructName}}(
{{#fields}}
{{#optional}}
      Option.make(this.{{gotName}}, this.{{_fieldName}}){{/optional}}
{{^optional}}
        this.{{_fieldName}}{{/optional}}
{{/fields|,
}}    );
    }
  }

  public Builder copy() {
    Builder builder = new Builder();
{{#fields}}
{{#optional}}
    if (this.{{fieldName}}.isDefined()) builder.{{fieldName}}(this.{{fieldName}}.get());
{{/optional}}
{{^optional}}
    builder.{{fieldName}}(this.{{fieldName}});
{{/optional}}
{{/fields}}
    return builder;
  }

  public static ThriftStructCodec<{{StructName}}> CODEC = new ThriftStructCodec3<{{StructName}}>() {
    @Override
    public {{StructName}} decode(TProtocol _iprot) throws org.apache.thrift.TException {
      Builder builder = new Builder();
{{#fields}}
      {{primitiveFieldType}} {{fieldName}} = {{defaultReadValue}};
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
              builder.{{fieldName}}({{fieldName}});
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

    @Override
    public void encode({{StructName}} struct, TProtocol oprot) throws org.apache.thrift.TException {
      struct.write(oprot);
    }
  };

  public static {{StructName}} decode(TProtocol _iprot) throws org.apache.thrift.TException {
    return CODEC.decode(_iprot);
  }

  public static void encode({{StructName}} struct, TProtocol oprot) throws org.apache.thrift.TException {
    CODEC.encode(struct, oprot);
  }

  public {{StructName}}(
{{#fields}}
    {{#optional}}Option<{{fieldType}}>{{/optional}}{{^optional}}{{primitiveFieldType}}{{/optional}} {{fieldName}}
{{/fields|, }}
  ) {
{{#fields}}
    this.{{fieldName}} = {{fieldName}};
{{/fields}}
  }

{{#alternativeConstructor}}
  public {{StructName}}(
{{#defaultFields}}
    {{primitiveFieldType}} {{fieldName}}
{{/defaultFields|, }}
  ) {
{{#fields}}
{{#optional}}
    this.{{fieldName}} = Option.none();
{{/optional}}
{{^optional}}
    this.{{fieldName}} = {{fieldName}};
{{/optional}}
{{/fields}}
  }
{{/alternativeConstructor}}

{{#fields}}
{{#hasGetter}}
  public {{primitiveFieldType}} {{getName}}() {
    return this.{{fieldName}}{{#optional}}.get(){{/optional}};
  }
{{/hasGetter}}
{{#hasIsDefined}}
  public boolean {{isSetName}}() {
{{#optional}}
    return this.{{fieldName}}.isDefined();
{{/optional}}
{{^optional}}
    return this.{{fieldName}} != null;
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
  if (this.{{fieldName}} == null)
      throw new org.apache.thrift.protocol.TProtocolException("Required field '{{fieldName}}' cannot be null");
{{/nullable}}
{{/required}}
{{/fields}}
  }

{{#hasExceptionMessage}}
  @Override
  public String getMessage() {
    return String.valueOf({{exceptionMessageField}});
  }
{{/hasExceptionMessage}}

  @Override
  public boolean equals(Object other) {
{{#arity0}}
    return this == other;
{{/arity0}}
{{^arity0}}
    if (!(other instanceof {{StructName}})) return false;
    {{StructName}} that = ({{StructName}}) other;
    return
{{#fields}}
{{^isPrimitive}}this.{{fieldName}}.equals(that.{{fieldName}}){{/isPrimitive}}
{{#isPrimitive}}
{{#optional}}
      this.{{fieldName}}.equals(that.{{fieldName}})
{{/optional}}
{{^optional}}
      this.{{fieldName}} == that.{{fieldName}}
{{/optional}}
{{/isPrimitive}}
{{/fields| &&
}};
{{/arity0}}
  }

  @Override
  public String toString() {
{{#arity0}}
    return "{{StructName}}()";
{{/arity0}}
{{^arity0}}
    return "{{StructName}}(" + {{#fields}}this.{{fieldName}}{{/fields| + "," + }} + ")";
{{/arity0}}
  }

  @Override
  public int hashCode() {
{{#arity0}}
    return super.hashCode();
{{/arity0}}
{{^arity0}}
    int hash = 1;
{{#fields}}
{{#isPrimitive}}
{{#optional}}
    hash = hash * (this.{{fieldName}}.isDefined() ? 0 : new {{fieldType}}(this.{{fieldName}}.get()).hashCode());
{{/optional}}
{{^optional}}
    hash = hash * new {{fieldType}}(this.{{fieldName}}).hashCode();
{{/optional}}
{{/isPrimitive}}
{{^isPrimitive}}
{{#optional}}
    hash = hash * (this.{{fieldName}}.isDefined() ? 0 : this.{{fieldName}}.get().hashCode());
{{/optional}}
{{^optional}}
    hash = hash * (this.{{fieldName}} == null ? 0 : this.{{fieldName}}.hashCode());
{{/optional}}
{{/isPrimitive}}
{{/fields}}
    return hash;
{{/arity0}}
  }
}