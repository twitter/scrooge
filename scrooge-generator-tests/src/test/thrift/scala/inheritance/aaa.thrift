#@namespace scala inheritance.aaa
namespace java inheritance.thriftjava.aaa
#@namespace typescript inheritance.thriftts.aaa

struct Box {
  1: i32 x
  2: i32 y
}

service Aaa {
  Box get_box()
}
