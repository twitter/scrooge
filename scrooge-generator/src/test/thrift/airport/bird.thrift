/* tests unions */
namespace java thrift.test

include "airport.thrift"

struct Raptor {
  1: bool isOwl
  2: string species
}

/** some kind of bird */
union Bird {
  1: Raptor raptor
  2: string hummingbird = "Calypte anna"
  3: string owlet_nightjar
  4: list<string> flock
  /** an airport bird...? (tests namespacing) */
  5: airport.Airport airport
}
