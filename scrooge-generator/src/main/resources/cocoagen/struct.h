#import <Foundation/Foundation.h>

#import "ApacheThrift/TBase.h"
{{headers}}

@interface {{StructName}} : NSObject <TBase, NSCoding>

{{#fields}}
@property (nonatomic{{^isPrimitive}}{{#readWriteInfo}}{{^isStruct}}, copy{{/isStruct}}{{/readWriteInfo}}{{/isPrimitive}}) {{fieldType}}{{#readWriteInfo}}{{#isStruct}}*{{/isStruct}}{{/readWriteInfo}} {{fieldNameCamelCase}};
@property (nonatomic, readonly) BOOL {{fieldNameCamelCase}}IsSet;

{{/fields}}

- (instancetype)initWith{{#fields}}{{fieldNameInInit}}:({{fieldType}}{{#readWriteInfo}}{{#isStruct}}*{{/isStruct}}{{/readWriteInfo}}){{fieldNameCamelCase}}{{/fields| }};
- (void)read:(id<TProtocol>)inProtocol;
- (void)write:(id<TProtocol>)outProtocol;

@end

