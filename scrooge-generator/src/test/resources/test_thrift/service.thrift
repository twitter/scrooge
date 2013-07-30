namespace java com.twitter.thrift

struct TestRequest {
  1: i32 id
}

exception TestException {
  1: string message
}

service TestServiceParent {
}

service TestService extends TestServiceParent {
  bool test_endpoint(1: TestRequest request) throws (1: TestException ex)
  oneway void test_oneway(1: TestRequest request)
}
