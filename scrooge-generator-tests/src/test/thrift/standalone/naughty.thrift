// Scala and Java keywords (but minus thrift keywords of course) are ok
namespace java thrift.def.default
const i32 val = 10 // `val` in Scala, val in Java
const i32 _try = 123 // `_try` in Scala, _try_ in Java

enum super_ { // rewritten to title case Super in generated code
  trait = 20  // `trait` in Scala, TRAIT in Java
  native_ = 99
}

struct TestValidate {
  1: optional bool validate
}

struct naughty { // rewritten to title case Naughty
  1: string type  // `type` in Scala; getType() in Java
  2: i32 abstract_ // `abstract_` in Scala, getAbstract_() in Java
  3: optional string runtime
  4: optional string scala
}

union NaughtyUnion {
  10: i32 value // test primitive type and field name "value"
  15: super_ field
  20: bool flag
  30: string text
}


// csl-389
struct fooResult {
  1: string message
}

service naughtyService {
  fooResult foo()
}

enum SameName {
  SameName = 1;
}
