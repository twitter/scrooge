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

{{#isUnion}}
{{#fields}}
- (instancetype)initWith{{FieldName}}:({{fieldType}}{{#readWriteInfo}}{{#isStruct}}*{{/isStruct}}{{/readWriteInfo}}){{fieldNameCamelCase}}
{
    if (self = [super init]) {
        [self set{{FieldName}}:{{fieldNameCamelCase}}];
    }
    return self;
}

{{/fields}}
{{/isUnion}}
{{^isUnion}}
{{#hasNonOptionalFields}}
- (instancetype)initWith{{#nonOptionalFields}}{{fieldNameInInit}}:({{fieldType}}{{#readWriteInfo}}{{#isStruct}}*{{/isStruct}}{{/readWriteInfo}}){{fieldNameCamelCase}}{{/nonOptionalFields| }}
{
    if (self = [super init]) {
{{#nonOptionalFields}}
        [self set{{FieldName}}:{{fieldNameCamelCase}}];
{{/nonOptionalFields}}
    }

    return self;
}

+ (instancetype)instanceWith{{#nonOptionalFields}}{{fieldNameInInit}}:({{fieldType}}{{#readWriteInfo}}{{#isStruct}}*{{/isStruct}}{{/readWriteInfo}}){{fieldNameCamelCase}}{{/nonOptionalFields| }} error:(NSError **)error
{
    {{StructName}} *instance = [[{{StructName}} alloc] initWith{{#nonOptionalFields}}{{fieldNameInInit}}:{{fieldNameCamelCase}}{{/nonOptionalFields| }}];
    if (error) {
        NSArray *invalidFields = [instance validateNonOptionalFields];
        if (invalidFields.count > 0) {
            NSString *errorDescription = [NSString stringWithFormat:@"Required fields not set: %@", invalidFields];
            *error = [NSError errorWithDomain:@"com.twitter.scrooge.backend.CocoaGenerator" code:0 userInfo:@{NSLocalizedDescriptionKey: errorDescription}];
        }
    }
    return instance;
}

{{/hasNonOptionalFields}}
{{/isUnion}}
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
{{#isPrimitive}}
    _{{fieldNameCamelCase}}IsSet = YES;
{{/isPrimitive}}
{{^isPrimitive}}
    _{{fieldNameCamelCase}}IsSet = {{fieldNameCamelCase}} != nil;
{{/isPrimitive}}
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

{{^isUnion}}
{{#hasNonOptionalFields}}
- (NSArray *)validateNonOptionalFields
{
    NSMutableArray *invalidFields = [NSMutableArray array];
{{#nonOptionalFields}}
    if (!_{{fieldNameCamelCase}}IsSet) {
        [invalidFields addObject:@"{{fieldNameCamelCase}}"];
    }
{{/nonOptionalFields}}
    return [invalidFields copy];
}

{{/hasNonOptionalFields}}
{{/isUnion}}
@end

