namespace java thrift.java.test1
#@namespace android thrift.complete.android.test1
namespace * thrift.star.test1

struct Simple {
  1: optional bool z
  2: optional byte b
  3: optional i16 s
  4: optional i32 i
  5: optional i64 j
  6: optional double d
  7: optional string str
  8: optional list<i32> i_list
  9: optional set<i32> i_set
  10: optional map<i32, i32> i_map
}

struct SimpleWithDefaults {
  1: optional bool z = true
  2: optional byte b = 1
  3: optional i16 s = 1
  4: optional i32 i = 1
  5: optional i64 j = 1
  6: optional double d = 1.0
  7: optional string str = "yo"
  8: optional list<i32> i_list = [1]
  9: optional set<i32> i_set = [1]
  10: optional map<i32, i32> i_map = {1: 1}
}


struct StructXA {
  1: optional i64 id
}

struct StructXB {
  1: optional i64 snake_case_field
  2: optional i64 camelCaseField
  3: optional string optional_field
  4: required i64 required_field
  5: optional StructXA struct_field
  6: optional i64 default_field = 10
}

const list<StructXB> ListOfComplexStructs = [
  {"snake_case_field": 1, "camelCaseField": 2, "optional_field": "val", "required_field": 3, "struct_field": {"id": 1}, "default_field": 1},
  {"snake_case_field": 71, "camelCaseField": 72, "required_field": 73, "struct_field": {"id": 71}, "default_field": 71},
  {"snake_case_field": 1, "camelCaseField": 2, "required_field": 3, "struct_field": {"id": 1}}
]