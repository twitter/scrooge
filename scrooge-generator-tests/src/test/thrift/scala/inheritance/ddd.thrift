#@namespace scala inheritance.ddd
namespace java inheritance.thriftjava.ddd

include "ccc.thrift"

service Ddd extends ccc.CccExtended {
  i32 delete(i32 input)
}
