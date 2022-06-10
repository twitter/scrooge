namespace java includes.b.thriftjava
#@namespace scala includes.b.thriftscala
#@namespace typescript includes.b.thriftts

include "const.thrift"

const const.TestName name1 = {
  "first" : "f1",
  "last" : "l1"
  "address": {
    "street": "some street"
    "city": {
      "city_state": {
        "city": "San Francisco"
        "state": "CA"
      }
    }
  }
}
