namespace java includes.a.thriftjava
#@namespace scala includes.a.thriftscala

struct CityState {
  1: string city
  2: string state
}

struct ZipCode {
  1: string zipcode
}

union City {
  1: CityState city_state
  2: ZipCode zipcode
}

struct Address {
  1: string street
  2: City city
}

struct TestName {
  1: string first
  2: string last
  3: string middle = ""
  4: Address address
}