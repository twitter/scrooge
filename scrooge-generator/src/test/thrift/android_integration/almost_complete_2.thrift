namespace java thrift.java.test2
#@namespace android thrift.complete.android.test2
namespace * thrift.star.test2

include "almost_complete_1.thrift"

struct ComplexCollections {
  1: optional list<almost_complete_1.StructXB> lxb,
  2: optional set<almost_complete_1.StructXB> sxb,
  3: optional map<almost_complete_1.StructXB, almost_complete_1.StructXB> mxb
  4: optional map<string, list<string>> complex_map,
  5: optional set<list<string>> complex_set,
  6: optional list<map<string, i32>> complex_list,
  7: optional list<set<map<string, list<string>>>> super_complex_collection
}

struct AlmostCompletTestStruct {
  1: required almost_complete_1.Simple notInitializedSimple,
  2: required almost_complete_1.Simple fullyInitializedSimple,
  3: required almost_complete_1.SimpleWithDefaults notInitializedSimpleWithDefaults,
  4: required almost_complete_1.SimpleWithDefaults fullyInitializedSimpleWithDefaults
  5: required ComplexCollections colls
}