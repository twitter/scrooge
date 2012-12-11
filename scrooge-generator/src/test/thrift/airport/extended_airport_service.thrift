
// give it a namespace different from the included files, otherwise the generated code
// will still compile even without namespace aliasing.
namespace java thrift.test
include "airport_service.thrift"
include "airport.thrift"

exception AirportException {
  1: i32 errorCode
}

service ExtendedAirportService extends airport_service.AirportService {
  bool hasWifi(1: airport.Airport a) throws (
    1: AirportException ex
  )
}

