#@namespace scala com.twitter.scrooge.backend.thriftscala
#@validator new com.twitter.scrooge.validation.JCustomValidator().getThriftValidator()

struct JCustomValidationStruct {
  1: string email (validation.length.min = "6", validation.email = "", validation.testStartWithA = "")
  2: string screenName (validation.testScreenName = "")
  3: i64 userId (validation.testUserId = "")
  4: i16 shortField (validation.negative = "")
  5: i8 byteField (validation.positive = "")
  6: map<string, string> mapField (validation.size.max = "1")
  7: bool boolField (validation.assertTrue = "")
}

struct JCustomNestedValidationStruct {
  1: string email (validation.email = "", validation.testStartWithA = "")
  2: JCustomValidationStruct nestedStructField
  // we don't support validating each struct in containers,
  // the annotations on a container type will be applied to
  // the container itself.
  3: list<JCustomValidationStruct> nestedStructSet (validation.size.max = "1")
}

union JCustomValidationUnion {
  1: i64 userId (validation.testUserId = "")
  2: string screenName (validation.notEmpty = "", validation.testScreenName = "")
}

exception JCustomValidationException {
  1: string msg (validation.notEmpty = "", validation.testStartWithA = "")
}
