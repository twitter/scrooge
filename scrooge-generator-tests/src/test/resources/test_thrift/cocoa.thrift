namespace cocoa TFNTwitterThriftScribe

enum TestEnum {
  FIELD_ONE = 1,
  FIELD_TWO = 2
}

struct TestStruct {
  1: string name
  2: set<string> values
  3: i32 price
}

struct AnotherTestStruct {
  1: list<TestStruct> structs
  2: list<string> stringStructs
  3: set<TestStruct> aSet
  4: map<string, TestStruct> aMap
  5: i32 id                         // primitive reserved keyword
  6: string protocol                // reference reserved keyword
  7: TestStruct SEL                 // struct upper-base reserved keyword
  8: i32 not_a_camel                // ensure correct field names
  9: TestEnum anEnum                // ensure correct enum references
  10: i16 shortNum                  // ensure can handle 16-bit ints
  11: i64 longLongNum               // ensure can handle 64-bit ints
}
