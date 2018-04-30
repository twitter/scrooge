{{headers}}

@import ApacheThrift.TBase;

@interface {{StructName}} : NSObject <TBase, NSCoding>

{{#fields}}
@property (nonatomic{{^isPrimitive}}{{#readWriteInfo}}{{^isStruct}}, copy{{/isStruct}}{{/readWriteInfo}}{{/isPrimitive}}) {{fieldType}}{{#readWriteInfo}}{{#isStruct}}*{{/isStruct}}{{/readWriteInfo}} {{fieldNameCamelCase}};
@property (nonatomic, readonly) BOOL {{fieldNameCamelCase}}IsSet;

{{/fields}}

{{#isUnion}}
{{#fields}}
- (instancetype)initWith{{FieldName}}:({{fieldType}}{{#readWriteInfo}}{{#isStruct}}*{{/isStruct}}{{/readWriteInfo}}){{fieldNameCamelCase}};
{{/fields}}
{{/isUnion}}
{{^isUnion}}
{{#hasNonOptionalFields}}
- (instancetype)initWith{{#nonOptionalFields}}{{fieldNameInInit}}:({{fieldType}}{{#readWriteInfo}}{{#isStruct}}*{{/isStruct}}{{/readWriteInfo}}){{fieldNameCamelCase}}{{/nonOptionalFields| }};
+ (instancetype)instanceWith{{#nonOptionalFields}}{{fieldNameInInit}}:({{fieldType}}{{#readWriteInfo}}{{#isStruct}}*{{/isStruct}}{{/readWriteInfo}}){{fieldNameCamelCase}}{{/nonOptionalFields| }} error:(NSError **)error;
{{/hasNonOptionalFields}}
{{/isUnion}}
- (void)read:(id<TProtocol>)inProtocol;
- (void)write:(id<TProtocol>)outProtocol;

@end

