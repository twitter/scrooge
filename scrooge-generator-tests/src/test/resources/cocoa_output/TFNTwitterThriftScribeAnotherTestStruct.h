/**
 * Generated by Scrooge
 *   version: ?
 *   rev: ?
 *   built at: ?
 *   source file: scrooge/scrooge-generator-tests/src/test/resources/test_thrift/cocoa.thrift
 */
#import <TFNTwitterThriftScribe/TFNTwitterThriftScribeTestEnum.h>
#import <TFNTwitterThriftScribe/TFNTwitterThriftScribeTestStruct.h>

@import ApacheThrift.TBase;

@interface TFNTwitterThriftScribeAnotherTestStruct : NSObject <TBase, NSCoding>

@property (nonatomic, copy) NSArray * structs;
@property (nonatomic, readonly) BOOL structsIsSet;

@property (nonatomic, copy) NSArray * stringStructs;
@property (nonatomic, readonly) BOOL stringStructsIsSet;

@property (nonatomic, copy) NSSet * aSet;
@property (nonatomic, readonly) BOOL aSetIsSet;

@property (nonatomic, copy) NSDictionary * aMap;
@property (nonatomic, readonly) BOOL aMapIsSet;

@property (nonatomic) int32_t id_;
@property (nonatomic, readonly) BOOL id_IsSet;

@property (nonatomic, copy) NSString * protocol_;
@property (nonatomic, readonly) BOOL protocol_IsSet;

@property (nonatomic) TFNTwitterThriftScribeTestStruct* sel_;
@property (nonatomic, readonly) BOOL sel_IsSet;

@property (nonatomic) int32_t notACamel;
@property (nonatomic, readonly) BOOL notACamelIsSet;

@property (nonatomic) TFNTwitterThriftScribeTestEnum anEnum;
@property (nonatomic, readonly) BOOL anEnumIsSet;

@property (nonatomic) int16_t shortNum;
@property (nonatomic, readonly) BOOL shortNumIsSet;

@property (nonatomic) int64_t longLongNum;
@property (nonatomic, readonly) BOOL longLongNumIsSet;

@property (nonatomic) int64_t constructionRequiredLongLong;
@property (nonatomic, readonly) BOOL constructionRequiredLongLongIsSet;


- (instancetype)initWithStructs:(NSArray *)structs stringStructs:(NSArray *)stringStructs aSet:(NSSet *)aSet aMap:(NSDictionary *)aMap id_:(int32_t)id_ protocol_:(NSString *)protocol_ sel_:(TFNTwitterThriftScribeTestStruct*)sel_ notACamel:(int32_t)notACamel anEnum:(TFNTwitterThriftScribeTestEnum)anEnum shortNum:(int16_t)shortNum longLongNum:(int64_t)longLongNum constructionRequiredLongLong:(int64_t)constructionRequiredLongLong;
+ (instancetype)instanceWithStructs:(NSArray *)structs stringStructs:(NSArray *)stringStructs aSet:(NSSet *)aSet aMap:(NSDictionary *)aMap id_:(int32_t)id_ protocol_:(NSString *)protocol_ sel_:(TFNTwitterThriftScribeTestStruct*)sel_ notACamel:(int32_t)notACamel anEnum:(TFNTwitterThriftScribeTestEnum)anEnum shortNum:(int16_t)shortNum longLongNum:(int64_t)longLongNum constructionRequiredLongLong:(int64_t)constructionRequiredLongLong error:(NSError **)error;
- (void)read:(id<TProtocol>)inProtocol;
- (void)write:(id<TProtocol>)outProtocol;

@end
