[outProtocol writeListBeginWithElementType:TType_{{eltWireConstType}} size:(int)[{{name}} count]];
for (int _{{name}}_i = 0; _{{name}}_i < [{{name}} count]; _{{name}}_i++) {
    {{eltType}}{{#eltReadWriteInfo}}{{#isStruct}} *{{/isStruct}}{{/eltReadWriteInfo}} {{eltName}} = {{name}}[_{{name}}_i];
{{#eltReadWriteInfo}}
    {{>writeValue}}
{{/eltReadWriteInfo}}
}
[outProtocol writeListEnd];