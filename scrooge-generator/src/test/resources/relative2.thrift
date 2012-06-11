namespace java thrift.test2

include "relative1.thrift"

typedef relative1.CandyType CandyType

struct Candy {
  1: i32 sweetness_iso
  2: CandyType candy_type
  3: optional string acceptHeader = relative1.DEFAULT_ACCEPT_HEADER
}
