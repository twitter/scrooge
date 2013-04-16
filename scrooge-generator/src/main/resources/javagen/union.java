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
public {{/public}}{{^public}}static {{/public}}class {{StructName}} implements ThriftStruct {
  private static final TStruct STRUCT = new TStruct("{{StructNameForWire}}");
{{#fields}}
  private static final TField {{fieldConst}} = new TField("{{fieldNameForWire}}", TType.{{constType}}, (short) {{id}});
  private final {{#optional}}Option<{{fieldType}}>{{/optional}}{{^optional}}{{fieldType}}{{/optional}} {{fieldName}};
{{/fields}}

  public enum Field {
{{#fields}}
    {{FIELD_NAME}}{{/fields|,
}}

  }

  /*
   * A flag indicate which field this union object is set to.
   * Note that it is ok to define a field with name "setField", the
   * generated getter method would be "getSetField", so there won't
   * be naming conflict.
   */
  public final Field setField;

  public static ThriftStructCodec<{{StructName}}> CODEC = new ThriftStructCodec3<{{StructName}}>() {
    public {{StructName}} decode(TProtocol _iprot) throws org.apache.thrift.TException {
      {{StructName}} result = null;
      _iprot.readStructBegin();
      TField _field = _iprot.readFieldBegin();
      switch (_field.id) {
{{#fields}}
{{#readWriteInfo}}
        {{>readUnionField}}
{{/readWriteInfo}}
{{/fields}}
        default:
          TProtocolUtil.skip(_iprot, _field.type);
      }
      if (_field.type != TType.STOP) {
        _iprot.readFieldEnd();
        Boolean _done = false;
        Boolean _moreThanOne = false;
        while (!_done) {
          _field = _iprot.readFieldBegin();
          if (_field.type == TType.STOP)
            _done = true;
          else {
            _moreThanOne = true;
            TProtocolUtil.skip(_iprot, _field.type);
            _iprot.readFieldEnd();
          }
        }
        if (_moreThanOne) {
          _iprot.readStructEnd();
          throw new TProtocolException("Cannot read a TUnion with more than one set value!");
        }
      }
      _iprot.readStructEnd();
      if (result == null)
        throw new TProtocolException("Cannot read a TUnion with no set value!");
      return result;
    }

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
    Field setField,
    Object value
  ) {
    if (value == null)
      throw new NullPointerException("Cannot construct {{StructName}} with a null value");
    this.setField = setField;
{{#fields}}
    this.{{fieldName}} = (setField == Field.{{FIELD_NAME}} ? ({{fieldType}}) value : null);
{{/fields}}
  }

{{#fields}}
  public static {{StructName}} {{newFieldName}}({{primitiveFieldType}} {{fieldName}}) {
    return new {{StructName}}(Field.{{FIELD_NAME}}, {{fieldName}});
  }
{{/fields}}

  public void write(TProtocol _oprot) throws org.apache.thrift.TException {
    _oprot.writeStructBegin(STRUCT);
    switch (setField) {
{{#fields}}
      case {{FIELD_NAME}}:
{{#readWriteInfo}}
      {{>writeField}}
{{/readWriteInfo}}
        break;
{{/fields}}
    }
    _oprot.writeFieldStop();
    _oprot.writeStructEnd();
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
    if (this.setField != that.setField) return false;
    switch (setField) {
{{#fields}}
      case {{FIELD_NAME}}:
{{^isPrimitive}}
        return this.{{fieldName}}.equals(that.{{fieldName}});
{{/isPrimitive}}
{{#isPrimitive}}
        return this.{{fieldName}} == that.{{fieldName}};
{{/isPrimitive}}
{{/fields}}
    }
    return false;
{{/arity0}}
  }

  @Override
  public String toString() {
{{#arity0}}
    return "{{StructName}}()";
{{/arity0}}
{{^arity0}}
    switch (setField) {
{{#fields}}
      case {{FIELD_NAME}}:
        return "{{StructName}}({{fieldName}}," + this.{{fieldName}} + ")";
{{/fields}}
    }
    return "{{StructName}}(???)";
{{/arity0}}
  }

  @Override
  public int hashCode() {
{{#arity0}}
    return super.hashCode();
{{/arity0}}
{{^arity0}}
    int hash = 1;
    switch (setField) {
{{#fields}}
      case {{FIELD_NAME}}:
{{#isPrimitive}}
        hash = hash * new {{fieldType}}(this.{{fieldName}}).hashCode();
{{/isPrimitive}}
{{^isPrimitive}}
        hash = hash * (this.{{fieldName}} == null ? 0 : this.{{fieldName}}.hashCode());
{{/isPrimitive}}
        break;
{{/fields}}
    }
    return hash;
{{/arity0}}
  }
}