
namespace java thrift.test


// Test struct names colliding with capitalized method names.
struct Result1 {
}

struct Result2 {
}

service CollidingService {
  Result1 result1();
  map<binary,Result2> result2();
}
