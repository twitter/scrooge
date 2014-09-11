// Ensures that types in different namespaces are imported and aliased properly in service code. If
// not, this won't compile.
namespace java thrift.test2

include "airport.thrift"
include "location.thrift"

service AirportService {
  list<airport.Airport> fetchAirportsInBounds(1: location.Location northWest, 2: location.Location southEast)
}
