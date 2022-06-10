#@namespace scala collisions.enums
namespace java collisions.enums_java
#@namespace typescript collisions.enums.thriftts

union Result {
  string successValue
  i32 errorCode
}

union Args_ {
  i32 why
  i64 why_not
}

service ServiceWithCollections {
  list<Result> query(Args_ a)
  map<Args_, list<Result>> nested()
  set<Result> moreNested(set<list<Args_>> a)
}
