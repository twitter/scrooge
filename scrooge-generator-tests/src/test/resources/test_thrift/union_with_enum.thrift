#@namespace android thrift.test.android
enum Day {
  Mon = 1,
  Tue = 2,
}

union TestUnion {
  1: i32 an_int
  2: string a_string
  3: set<string> a_set
  4: Day day = 1
}
