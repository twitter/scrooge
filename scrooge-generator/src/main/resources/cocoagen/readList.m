int _{{name}}_size;
[inProtocol readListBeginReturningElementType:NULL size:&_{{name}}_size];
NSMutableArray *{{name}}_mutable = [[NSMutableArray alloc] initWithCapacity:_{{name}}_size];
for (int _{{name}}_i = 0; _{{name}}_i < _{{name}}_size; ++_{{name}}_i) {
    {{eltType}}{{#eltReadWriteInfo}}{{#isStruct}} *{{/isStruct}}{{/eltReadWriteInfo}} {{eltName}};
{{#eltReadWriteInfo}}
    {{>readValue}}
{{/eltReadWriteInfo}}
    [{{name}}_mutable addObject: {{eltName}}];
}
{{name}} = {{name}}_mutable;
[inProtocol readListEnd];