namespace java thrift.defaults

struct Foo {
  1: string field1
  2: i32 field2
}

const map<string, Foo> Foos = {
  "identifier": {"field1": "field1 val", "field2": 123},
}
