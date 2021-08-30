#@namespace scala inheritance.ddd
namespace java inheritance.thriftjava.ddd

include "ccc.thrift"

service Ddd extends ccc.CccExtended {
  i32 remove(i32 input)
}
