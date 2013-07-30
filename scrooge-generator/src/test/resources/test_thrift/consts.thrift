namespace java com.twitter.thrift

enum TestEnum {
  foo = 1,
  bar = 2,
}

const i32 INT32CONSTANT = 9853
const TestEnum TESTENUMCONSTANT = 1
const bool BOOLCONST = 0
const bool BOOLCONST2 = 1
const double DOUBLECONST = 10.5
const double DOUBLECONST2 = 9
const i64 LONGCONST = 100
const i64 LONGCONST2 = 8589934592
const TestEnum ENUMCONST = 2
const map<map<TestEnum, string>, string> MAPCONSTANT = {{1 :'world', 2 :'moon'} : 'hello'}
const set<list<string>> SETCONSTANT = [['item1', 'item2']]
