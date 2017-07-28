#import "{{StructName}}.h"

@import ApacheThrift;

@implementation {{StructName}}

- (NSString*)description
{
    NSMutableString* ms = [NSMutableString stringWithString:@"{{StructName}}( "];
{{#fields}}
    [ms appendString:@"{{fieldNameCamelCase}}:"];
    [ms appendFormat:@"%@ ", {{#isPrimitive}}@({{/isPrimitive}}_{{fieldNameCamelCase}}{{#isPrimitive}}){{/isPrimitive}}];
{{/fields}}
    [ms appendString:@")"];
    return [NSString stringWithString:ms];
}

- (instancetype)initWith{{#fields}}{{fieldNameInInit}}:({{fieldType}}{{#readWriteInfo}}{{#isStruct}}*{{/isStruct}}{{/readWriteInfo}}){{fieldNameCamelCase}}{{/fields| }}
{
    if (self = [super init]) {
{{#fields}}
        [self set{{FieldName}}:{{fieldNameCamelCase}}];
{{/fields}}
    }

    return self;
}

- (instancetype)initWithCoder:(NSCoder*)decoder
{
    if (self = [super init]) {
{{#fields}}
        if ([decoder containsValueForKey:@"{{id}}"]) {
            [self set{{FieldName}}:({{fieldType}}{{#readWriteInfo}}{{#isStruct}}*{{/isStruct}}{{/readWriteInfo}})[decoder {{decodeMethod}}:@"{{id}}"]];
        }
{{/fields}}
    }
    return self;
}

- (void)encodeWithCoder:(NSCoder*)encoder
{
{{#fields}}
    if (_{{fieldNameCamelCase}}IsSet) {
        [encoder {{encodeMethod}}:_{{fieldNameCamelCase}} forKey:@"{{id}}"];
    }
{{/fields}}
}

{{#fields}}
- (void)set{{FieldName}}:({{fieldType}}{{#readWriteInfo}}{{#isStruct}} *{{/isStruct}}{{/readWriteInfo}}){{fieldNameCamelCase}}
{
    _{{fieldNameCamelCase}} = {{^isPrimitive}}{{#readWriteInfo}}{{^isStruct}}[{{/isStruct}}{{/readWriteInfo}}{{/isPrimitive}}{{fieldNameCamelCase}}{{^isPrimitive}}{{#readWriteInfo}}{{^isStruct}} copy]{{/isStruct}}{{/readWriteInfo}}{{/isPrimitive}};
    _{{fieldNameCamelCase}}IsSet = YES;
}
{{/fields|
}}

- (void)read:(id <TProtocol>)inProtocol
{
    NSString* fieldName;
    int fieldType;
    int fieldID;

    [inProtocol readStructBeginReturningName:nil];
    while (true) {
        [inProtocol readFieldBeginReturningName:&fieldName type:&fieldType fieldID:&fieldID];
        if (fieldType == TType_STOP) {
            break;
        }
        switch (fieldID) {
{{#fields}}
{{#readWriteInfo}}
            {{>readField}}
{{/readWriteInfo}}
{{/fields}}
        default:
            NSLog(@"%s: unexpected field ID %i with type %i.  Skipping.", __PRETTY_FUNCTION__, fieldID, fieldType);
            [TProtocolUtil skipType:fieldType onProtocol:inProtocol];
            break;
        }
        [inProtocol readFieldEnd];
    }
    [inProtocol readStructEnd];
    [self validate];
}

- (void)write:(id <TProtocol>)outProtocol
{
    [self validate];
    [outProtocol writeStructBeginWithName:@"{{StructName}}"];
{{#fields}}
{{#readWriteInfo}}
    {{>writeField}}
{{/readWriteInfo}}
{{/fields}}
    [outProtocol writeFieldStop];
    [outProtocol writeStructEnd];
}

- (void)validate
{
{{#fields}}
{{#required}}
    if (!_{{fieldNameCamelCase}}IsSet) {
        @throw [TProtocolException exceptionWithName:@"TProtocolException" reason:@"Required field '{{fieldNameCamelCase}}' is not set."];
    }
{{/required}}
{{/fields}}
}

@end

