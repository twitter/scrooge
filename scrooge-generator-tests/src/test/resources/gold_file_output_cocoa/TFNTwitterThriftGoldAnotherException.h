/**
 * Generated by Scrooge
 *   version: ?
 *   rev: ?
 *   built at: ?
 *   source file: scrooge/scrooge-generator-tests/src/test/resources/gold_file_input/gold.thrift
 */


@import ApacheThrift.TBase;

@interface TFNTwitterThriftGoldAnotherException : NSObject <TBase, NSCoding>

@property (nonatomic) int32_t errorCode;
@property (nonatomic, readonly) BOOL errorCodeIsSet;


- (instancetype)initWithErrorCode:(int32_t)errorCode;
+ (instancetype)instanceWithErrorCode:(int32_t)errorCode error:(NSError **)error;
- (void)read:(id<TProtocol>)inProtocol;
- (void)write:(id<TProtocol>)outProtocol;

@end
