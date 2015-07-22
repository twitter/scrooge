if (_{{fieldNameCamelCase}}IsSet) {
    [outProtocol writeFieldBeginWithName:@"{{fieldNameForWire}}" type:TType_{{wireConstType}} fieldID:{{id}}];
    {{fieldType}}{{#readWriteInfo}}{{#isStruct}}*{{/isStruct}}{{/readWriteInfo}} {{valueVariableName}} = _{{fieldNameCamelCase}};
    {{>writeValue}}
    [outProtocol writeFieldEnd];
}
