namespace cocoa TFNTwitterThriftGold
namespace java com.twitter.scrooge.test.gold.thriftjava
#@namespace scala com.twitter.scrooge.test.gold.thriftscala
#@namespace android com.twitter.scrooge.test.gold.thriftandroid

typedef i64 CollectionLongId

exception OverCapacityException {
  1: i32 chillTimeSeconds (e.field.annotation = "false")
  }(e.annotation = "true")

exception AnotherException {
  1: i32 errorCode
}

enum RequestType {
  Create = 1 (some.annotation = "true"),
  Read = 2,
} (enum.annotation = "false")

union ResponseUnion {
  1: i64 id
  2: string details (u.field.annotation = "x")
} (u.annotation = "y")

struct CollectionId {
  1: required CollectionLongId collectionLongId;
}

struct Recursive {
  1: i64 id
  2: optional Request recRequest
}

/**
 * Request struct docstring
 */
struct Request {
  1: list<string> aList,
  2: set<i32> aSet,
  3: map<i64, i64> aMap,
  4: optional Request aRequest,
  5: list<Request> subRequests,
  6: string _default = "the_default"

  7: optional i64 noComment

  // ignored double slash comment
  8: optional i64 doubleSlashComment

  # ignored hashtag comment
  9: optional i64 hashtagComment (a.b.c = "ignored")

  /*
   * ignored single asterisk comment
   */
  10: optional i64 singleAsteriskComment (s.field.annotation.one = "a", two = "b")

  /**
   * docstring comment
   */
  11: optional i64 docStringComment

  /**
   * recursive value
   */
  12: optional Recursive recRequest

  13: required string requiredField
  14: optional i64 constructionRequiredField (construction_required = "true")
  15: optional i8 anInt8
  16: optional binary aBinaryField
} (s.annotation.one = "something",
  s.annotation.two = "other",
  com.twitter.scrooge.scala.generateStructProxy = "true")

struct Response {
  1: i32 statusCode,
  2: ResponseUnion responseUnion
} (com.twitter.scrooge.scala.generateStructProxy = "true")

service GoldService {

  /** Hello, I'm a comment. */
  Response doGreatThings(
    1: Request request
  ) throws (
    1: OverCapacityException ex
  ) (some.annotation = "false")

  Response noExceptionCall(
    1: Request request
  )

} (an.annotation = "true")

service PlatinumService extends GoldService {
  i32 moreCoolThings(
    1: Request request
  ) throws (
    1: AnotherException ax,
    2: OverCapacityException oce
  )
}
