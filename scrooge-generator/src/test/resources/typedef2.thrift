namespace java thrift.typedef2

include "typedef1.thrift"

struct IntCollection {
  1: typedef1.ManyInts scores1
  2: set<typedef1.OneInt> scores2
}
