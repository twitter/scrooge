namespace java com.twitter.other

include "service.thrift"

service OtherService extends service.TestServiceParent {
  bool test_endpoint(1: i32 req)
}
