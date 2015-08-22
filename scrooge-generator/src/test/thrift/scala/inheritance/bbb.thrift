#@namespace scala inheritance.bbb
namespace java inheritance.thriftjava.bbb

include "aaa.thrift"

service Bbb extends aaa.Aaa {
  i32 getInt()
}
