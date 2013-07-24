namespace java com.twitter.thrift

struct TestRequest {
  1: i32 id
}

struct TestResult {
  1: string message
}

exception TestException {
  1: string message
}

service TestService {
  void test_void_endpoint(1: TestRequest request) throws (1: TestException ex)
  TestResult test_endpoint_without_exception(1: TestRequest request)
}
