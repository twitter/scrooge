#@namespace scala collisions
namespace java collisions_java
#@namespace typescript collisions.thriftts

# Check for collisions between Result and MyService.MyMethod.Result
struct Result {
  1: bool success
}

struct Args_ {
  1: bool success
}

service MyService {
  Result getMethod()
  i32 setMethod(1: Result result)
  Args_ anotherMethod(1: Args_ a)
}


service ServiceWithCollections {
  list<Result> query(Args_ a)
  map<Args_, list<Result>> nested()
  set<Result> moreNested(set<list<Args_>> a)
}

