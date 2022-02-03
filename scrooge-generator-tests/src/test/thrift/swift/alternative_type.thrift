namespace java com.twitter.thrift.encoding
#@namespace scala com.twitter.thrift.encoding.thriftscala
#@namespace swift ThriftTypeEncoding

struct NonrenamedStruct {
  1: required RenamedStruct enumValue
}

struct RenamedStruct {
  1: required i32 intValue
  2: required RenamedUnion unionValue
}(alternative.type='ThriftStruct')

union RenamedUnion {
  1: i32 intValue
  2: RenamedEnum enumValue
}(alternative.type='ThriftUnion')

enum RenamedEnum {
  ONE = 1
}(alternative.type='ThriftEnum')


