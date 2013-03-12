namespace * com.fake

include "namespace_none.thrift"
include "namespace_single.thrift"

service Restaurant extends namespace_single.Business {
  i32 makeReservation(1: namespace_none.Weekday whichDay, 2: i32 howMany)
}