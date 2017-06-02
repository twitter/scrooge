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

struct Flight {
  1: required i64 id
  2: required string name
  3: required Airport from
  4: required Airport to
  5: optional i64 departureTime
}

struct Airline {
  1: required string name
  2: optional Airport headQuarter
  3: optional string  owner
  4: optional set<Airport> airports
  5: optional map<Airport, Airport> routes
  6: optional set<Flight> flights
}
