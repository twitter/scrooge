#@namespace scala com.twitter.scrooge.serializer.thriftscala
namespace java com.twitter.scrooge.serializer.thriftjava

struct SerializerTest {
  1: required i32 intField
}

struct SerializerStringTest {
  1: required string strField
}

struct SerializerListTest {
  1: required list<i32> listField
}

struct SerializerSetTest {
  1: required set<i32> setField
}

struct SerializerMapTest {
  1: required map<i32, i32> mapField
}
