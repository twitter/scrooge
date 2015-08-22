#@namespace scala collisions
namespace java collisions_java

# Check for collisions between Result and MyService.MyMethod.Result
struct Result {
  1: bool success
}

struct Args {
  1: bool success
}

service MyService {
  Result getMethod()
  i32 setMethod(1: Result result)
  Args anotherMethod(1: Args a)
}


service ServiceWithCollections {
  list<Result> query(Args args)
  map<Args, list<Result>> nested()
  set<Result> moreNested(set<list<Args>> args)
}

