#@namespace scala inheritance.bbb
namespace java inheritance.thriftjava.bbb
#@namespace typescript inheritance.thriftts.bbb

include "aaa.thrift"

service Bbb extends aaa.Aaa {
  i32 getInt()
}
