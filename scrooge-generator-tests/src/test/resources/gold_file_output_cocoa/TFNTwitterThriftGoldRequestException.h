/**
 * Generated by Scrooge
 *   version: ?
 *   rev: ?
 *   built at: ?
 */


@import ApacheThrift.TBase;

@interface TFNTwitterThriftGoldRequestException : NSObject <TBase, NSCoding>

@property (nonatomic, copy) NSString * message;
@property (nonatomic, readonly) BOOL messageIsSet;


- (instancetype)initWithMessage:(NSString *)message;
+ (instancetype)instanceWithMessage:(NSString *)message error:(NSError **)error;
- (void)read:(id<TProtocol>)inProtocol;
- (void)write:(id<TProtocol>)outProtocol;

@end