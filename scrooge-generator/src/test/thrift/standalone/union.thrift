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