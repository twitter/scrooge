namespace cocoa TFNTwitterThriftGold
namespace java com.twitter.scrooge.test.gold.thriftjava
#@namespace scala com.twitter.scrooge.test.gold.thriftscala
#@namespace android com.twitter.scrooge.test.gold.thriftandroid

exception OverCapacityException {
  1: i32 chillTimeSeconds
}

enum RequestType {
  Create = 1,
  Read = 2,
}

union ResponseUnion {
  1: i64 id
  2: string details
}

struct CollectionId {
  1: required CollectionId collectionId;
}

struct Request {
  1: list<string> aList,
  2: set<i32> aSet,
  3: map<i64, i64> aMap,
  4: optional Request aRequest,
  5: list<Request> subRequests
}

struct Response {
  1: i32 statusCode,
  2: ResponseUnion responseUnion
}

service GoldService {

  /** Hello, I'm a comment. */
  Response doGreatThings(
    1: Request request
  ) throws (
    1: OverCapacityException ex
  )

}
