case {{id}}:
    if (fieldType == TType_{{wireConstType}}) {
        {{fieldType}}{{#readWriteInfo}}{{#isStruct}}*{{/isStruct}}{{/readWriteInfo}} {{valueVariableName}};
        {{>readValue}}
        [self set{{FieldName}}:{{valueVariableName}}];
    } else {
        NSLog(@"%s: field ID %i has unexpected type %i.  Skipping.", __PRETTY_FUNCTION__, fieldID, fieldType);
        [TProtocolUtil skipType:fieldType onProtocol:inProtocol];
    }
    break;
