#@namespace scala inheritance.ddd
namespace java inheritance.java.ddd

include "ccc.thrift"

service Ddd extends ccc.CccExtended {
  i32 delete(i32 input)
}
