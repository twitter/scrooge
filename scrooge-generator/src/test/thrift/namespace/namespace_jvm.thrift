namespace scala com.fake.scala_ns
namespace java com.fake.java_ns

// without support for the scala namespace this will cause
// a compile error
struct MultiNamespaced {
  1: string ns
}
