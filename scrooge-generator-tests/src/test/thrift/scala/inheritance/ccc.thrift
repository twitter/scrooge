#@namespace scala inheritance.ccc
namespace java inheritance.thriftjava.ccc
#@namespace typescript inheritance.thriftts.ccc

include "bbb.thrift"

service Ccc extends bbb.Bbb {
  i32 setInt(i32 input)
}

service CccExtended extends Ccc {
  i32 iiii()
}
