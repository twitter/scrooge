namespace java thrift.benchmark
#@namespace scala thrift.benchmark

struct Location {
  1: required double latitude
  2: required double longitude
  3: optional double altitude
}

struct Airport {
  1: required string code
  2: required string name
  3: optional string country
  4: optional string state
  5: optional string closest_city
  6: optional Location loc
}
