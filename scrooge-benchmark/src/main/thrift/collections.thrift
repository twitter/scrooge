namespace java thrift.benchmark

struct MapCollections {
  1: map<i64, string> intString
}

struct SetCollections {
  1: set<i64> longs
}

struct ListCollections {
  1: list<i64> longs
}
