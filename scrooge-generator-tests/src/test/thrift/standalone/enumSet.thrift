namespace * thrift.test

enum ReturnCode {
  Good,
  Bad,
  VeryBad
}

struct StructWithEnumSet {
  1: set<ReturnCode> codes,
  2: set<ReturnCode> codesWithDefault = [ReturnCode.Good]
}
