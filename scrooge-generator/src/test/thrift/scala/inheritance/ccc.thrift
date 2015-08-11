#@namespace scala inheritance.ccc
namespace java inheritance.java.ccc

include "bbb.thrift"

service Ccc extends bbb.Bbb {
  i32 setInt(i32 input)
}

service CccExtended extends Ccc {
  i32 iiii()
}
