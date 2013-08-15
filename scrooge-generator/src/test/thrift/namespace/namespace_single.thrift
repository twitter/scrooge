namespace java foo

include "namespace_none.thrift"

service Business {
  bool isOpen(1: namespace_none.Weekday day)
}
