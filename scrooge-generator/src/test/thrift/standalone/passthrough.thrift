namespace java thrift.test

struct PassThroughStruct {
  optional i32 f1
  optional i32 f2
}

struct PassThrough {
  1: i32 f1
}

struct PassThrough2 {
  1: i32 f1,
  2: PassThroughStruct f2
  3: PassThroughStruct f3
}

// Ensure services properly compile
service PassThroughService {
  PassThrough doPassthrough(1: PassThrough2 obj)
}
