namespace java thrift.test

struct PassThrough {
  1: i32 f1
}

struct PassThrough2 {
  1: i32 f1,
  2: i32 f2
}

struct PassThrough3 {
  i32 f1
  i32 f2
}

// Ensure services properly compile
service PassThroughService {
  PassThrough doPassthrough(1: PassThrough2 obj)
}
