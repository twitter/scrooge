namespace java thrift.test2

include "typedef1.thrift"

struct IntCollection {
  1: typedef1.ManyInts scores1
  2: set<typedef1.OneInt> scores2
}
