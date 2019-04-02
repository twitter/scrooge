namespace java thrift.defaults

struct StructA {
  1: i64 id
}

struct StructB {
  1: i64 snake_case_field
  2: i64 camelCaseField
  3: optional string optional_field
  4: required i64 required_field
  5: optional StructA struct_field
  6: i64 default_field = 10
}

const map<string, StructA> MapOfStructVals = {
  "key1" : {"id": 1},
  "key2" : {"id": 2}
}

const map<StructA, string> MapOfStructKeys = {
  {"id": 1} : "val1",
  {"id": 2} : "val2",
}

const map<StructA, StructA> MapOfStructKeyVals = {
  {"id": 1} : {"id": 1},
  {"id": 2} : {"id": 2},
}

const list<StructA> ListOfStructs = [
  {"id": 1},
  {"id": 2},
  {"id": 2}
]

const set<StructA> SetOfStructs = [
  {"id": 1},
  {"id": 2},
  {"id": 2}
]

const StructA DEFAULT_STRUCT = {"id": 1}

const list<StructB> ListOfComplexStructs = [
  {"snake_case_field": 1, "camelCaseField": 2, "optional_field": "val", "required_field": 3, "struct_field": {"id": 1}, "default_field": 1},
  {"snake_case_field": 1, "camelCaseField": 2, "required_field": 3, "struct_field": {"id": 1}, "default_field": 1},
  {"snake_case_field": 1, "camelCaseField": 2, "required_field": 3, "struct_field": {"id": 1}}
]

union SimpleUnion {
  1: i32 a,
  2: string b
  3: string STRING
}

const SimpleUnion sss = { "a": 3 }

union ComplexUnion {
  1: StructA a,
  2: StructB b
}

const ComplexUnion ccc = {
  "b": {
    "snake_case_field": 1,
    "camelCaseField": 2,
    "required_field": 3,
    "struct_field": {"id": 1}
  }
}
