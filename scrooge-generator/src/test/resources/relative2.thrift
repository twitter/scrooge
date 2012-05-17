namespace java thrift.test

include "relative1.thrift"

typedef relative1.CandyType CandyType

struct Candy {
  1: i32 sweetness_iso
  2: CandyType candy_type
}
