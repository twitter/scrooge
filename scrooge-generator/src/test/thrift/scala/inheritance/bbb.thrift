#@namespace scala inheritance.bbb
namespace java inheritance.java.bbb

include "aaa.thrift"

service Bbb extends aaa.Aaa {
  i32 getInt()
}
