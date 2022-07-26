namespace java com.twitter.scrooge.finagle_integration.thriftjava
#@namespace scala com.twitter.scrooge.finagle_integration.thriftscala

exception InvalidQueryException {
  1: i32 errorCode
}

struct RegressionStruct {
  1: required list<string> (python.immutable = "") list_of_strings
} (python.immutable = "")

service BarService {
  string echo(1: string x) throws (
    1: InvalidQueryException ex
  )

  string duplicate(1: string y) throws (
    1: InvalidQueryException eex
  )

  string getDuck(1: i64 key)

  void setDuck(1: i64 key, 2: string value) throws (
    1: InvalidQueryException sx
  )

  RegressionStruct regression(
    1: optional set<string> (python.immutable = "") arg
  ) throws (
    1: InvalidQueryException regression_ex
  )
}

service ExtendedBarService extends BarService {
  string triple(1: string z) throws (
    1: InvalidQueryException eeex
  )
}
