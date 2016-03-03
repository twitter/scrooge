#@namespace scala collisions.enums
namespace java collisions.enums_java

union Result {
  string successValue
  i32 errorCode
}

union Args {
  i32 why
  i64 not
}

service ServiceWithCollections {
  list<Result> query(Args args)
  map<Args, list<Result>> nested()
  set<Result> moreNested(set<list<Args>> args)
}
