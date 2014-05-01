namespace java thrift.test

struct OldUnionField {
  1: i64 oldValue
}

struct SomeInnerUnionStruct {
  1: i32 anInt
  2: string aString
}

struct NewUnionField {
  1: i32 newValue
  2: SomeInnerUnionStruct innerStruct
}

union UnionPreEvolution {
  1: OldUnionField oldField
}

union UnionPostEvolution {
  1: OldUnionField oldField
  2: NewUnionField newField
}

struct MatchingStructField { 1: i64 id }
struct MatchingStructList { 1: i64 id }
struct MatchingStructSet { 1: i64 id }
struct MatchingStructMap { 1: i64 id }

union MatchingFieldAndStruct {
  1: MatchingStructField matchingStructField
  2: list<MatchingStructList> matchingStructList
  3: set<MatchingStructSet> matchingStructSet
  4: map<MatchingStructMap, MatchingStructMap> matchingStructMap
}
