namespace java com.twitter.thrift.encoding
#@namespace scala com.twitter.thrift.encoding.thriftscala
#@namespace swift ThriftEncoding

struct FoundationThriftStruct {
  1: required bool boolValue
  2: required double doubleValue
  3: required i16 int16Value
  4: required i32 int32Value
  5: required i64 int64Value
  6: required string stringValue
}

struct OptionalThriftStruct {
  1: optional i16 int16Value
} (alternative.type = 'ThriftStruct')

struct SubobjectThriftStruct {
  1: optional OptionalThriftStruct value
  2: required i16 intValue
}

struct CollectiontThriftStruct {
  1: optional list<double> arrays
  2: optional map<string, string> maps
  3: optional set<i32> sets
}

struct UnionClassA {
  1: required string someString
}
struct UnionClassB {
  1: required i64 someInt
}
union MyUnion {
  1: UnionClassA unionClassA
  2: UnionClassB unionClassB
}
struct UnionStruct {
  1: required MyUnion someUnion
}

enum SomeEnum {
  AAA = 1,
  BBB = 2
}

struct EnumStruct {
  1: required SomeEnum enumValue
}

enum Day {
  Mon = 1,
  Tue = 2,
}

union TestUnion {
  1: i32 an_int
  2: string a_string
  3: set<string> a_set
  4: Day day
}

const map<i64, i64> i64_i64_map = {
  1 : 1,
  2147483648 : 2147483648,
  -2147483649 : -2147483649
};

const set<i64> i64_set = [ 1, 2147483648, -2147483649 ];

const list<i64> i64_list = [ 1, 2147483648, -2147483649 ];

struct StructWithMap {
  1: map<string, i32> data,
  2: list<i32> emptylist
}

const StructWithMap constWithRHS = {
  "data": { "a": 1, "b": 2 },
  "emptylist": []
}
