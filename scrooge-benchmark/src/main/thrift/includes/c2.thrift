
include "b1.thrift"


service MyService2 {
  b1.MyEnum getEnumValue(1: i32 aaaa )
}
