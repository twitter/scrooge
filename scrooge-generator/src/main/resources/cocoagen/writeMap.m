[outProtocol writeMapBeginWithKeyType: TType_{{keyWireConstType}} valueType: TType_{{valueWireConstType}} size: (int)[{{name}} count]];
for (id {{keyName}}_id in {{name}}) {
    {{keyType}}{{#keyReadWriteInfo}}{{#isStruct}} *{{/isStruct}}{{/keyReadWriteInfo}} {{keyName}};
    {{valueType}}{{#valueReadWriteInfo}}{{#isStruct}} *{{/isStruct}}{{/valueReadWriteInfo}} {{valueName}};
    id {{valueName}}_id = [{{name}} objectForKey:{{keyName}}_id];
    {{keyName}} = {{keyGetValueMethod}};
    {{valueName}} = {{valueGetValueMethod}};
{{#keyReadWriteInfo}}
    {{>writeValue}}
{{/keyReadWriteInfo}}
{{#valueReadWriteInfo}}
    {{>writeValue}}
{{/valueReadWriteInfo}}
}
[outProtocol writeMapEnd];
