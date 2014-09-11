namespace java thrift.test

struct PassThroughStruct {
  optional i32 f1
  optional i32 f2
}

struct PassThrough {
  1: i32 f1
}

struct PassThrough2 {
  1: i32 f1
  2: PassThroughStruct f2
  3: PassThroughStruct f3
}

struct PassThrough3 {
  1: PassThrough2 f1
}

struct PassThrough4 {
  1: PassThrough f1
}

union PassThroughUnion1 {
  1: PassThrough2 f1
}

union PassThroughUnion2 {
  1: PassThrough f1
}

struct PassThrough5 {
  1: PassThroughUnion1 f1
}

struct PassThrough6 {
  1: PassThroughUnion2 f1
}

// Ensure services properly compile
service PassThroughService {
  PassThrough doPassthrough(1: PassThrough2 obj)
}
