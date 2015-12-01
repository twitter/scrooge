namespace java includes.a.thriftjava
#@namespace scala includes.a.thriftscala

struct Address {
  1: string street
  2: string city
  3: string state
}

struct TestName {
  1: string first
  2: string last
  3: string middle = ""
  4: Address address
}