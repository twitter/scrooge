[outProtocol writeSetBeginWithElementType:TType_{{eltWireConstType}} size:(int)[{{name}} count]];
for (id {{eltName}} in {{name}}) {
{{#eltReadWriteInfo}}
    {{>writeValue}}
{{/eltReadWriteInfo}}
}
[outProtocol writeSetEnd];