int _{{name}}_size;
[inProtocol readMapBeginReturningKeyType: NULL valueType: NULL size: &_{{name}}_size];
NSUInteger _{{name}}_capacity= _{{name}}_size < 0 ? 0 : (NSUInteger)_{{name}}_size;
NSMutableDictionary *{{name}}_mutable = [[NSMutableDictionary alloc] initWithCapacity: _{{name}}_capacity];
for (int _{{name}}_i = 0; _{{name}}_i < _{{name}}_size; ++_{{name}}_i) {
    {{keyType}}{{#keyReadWriteInfo}}{{#isStruct}} *{{/isStruct}}{{/keyReadWriteInfo}} {{keyName}};
    {{valueType}}{{#valueReadWriteInfo}}{{#isStruct}} *{{/isStruct}}{{/valueReadWriteInfo}} {{valueName}};
{{#keyReadWriteInfo}}
    {{>readValue}}
{{/keyReadWriteInfo}}
{{#valueReadWriteInfo}}
    {{>readValue}}
{{/valueReadWriteInfo}}
    [{{name}}_mutable setObject:{{valueName}} forKey:{{#isKeyPrimitive}}@({{/isKeyPrimitive}}{{keyName}}{{#isKeyPrimitive}}){{/isKeyPrimitive}}];
}
{{name}} = {{name}}_mutable;
[inProtocol readMapEnd];
