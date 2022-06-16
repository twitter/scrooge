#@namespace scala com.twitter.scrooge.backend.thriftscala

struct ValidationStruct {
  1: string stringField (validation.length.min = "6", validation.email = "")
  2: i32 intField (validation.positiveOrZero = "")
  3: i64 longField (validation.max = "100")
  4: i16 shortField (validation.negative = "")
  5: i8 byteField (validation.positive = "")
  6: map<string, string> mapField (validation.size.max = "1")
  7: bool boolField (validation.assertTrue = "")
  8: required string requiredField
  9: optional string optionalField
}

// a struct without any annotations
struct NoValidationStruct {
  1: string stringField
  2: i32 intField
  3: i64 longField
  4: i16 shortField
  5: i8 byteField
  6: map<string, string> mapField
  7: bool boolField
  8: required string requiredField
  9: optional string optionalField
}

// skip annotations not used for ThriftValidator
struct NonValidationStruct {
  1: string stringField (structFieldKey = "")
}

struct NestedNonValidationStruct {
 1: string stringField
 2: ValidationStruct nestedStruct
}

struct DeepNestedValidationstruct {
 1: string stringField
 2: NestedNonValidationStruct deepNestedStruct
}

struct NestedValidationStruct {
  1: string stringField (validation.email = "")
  2: ValidationStruct nestedStructField
  // we don't support validating each struct in containers,
  // the annotations on a container type will be applied to
  // the container itself.
  3: list<ValidationStruct> nestedStructSet (validation.size.max = "1")
}

union ValidationUnion {
  1: i32 unionIntField (validation.positiveOrZero = "")
  2: string unionStringField (validation.notEmpty = "")
}

exception ValidationException {
  1: string excField (validation.notEmpty = "")
}

service ValidationService {
  bool validate(
    1: ValidationStruct structRequest,
    2: ValidationUnion unionRequest,
    3: ValidationException exceptionRequest
  )
  bool validateOption (
    1: optional ValidationStruct structRequest,
    2: optional ValidationUnion unionRequest,
    3: optional ValidationException exceptionRequest
  )
  bool validateWithNonValidatedRequest (
    1: ValidationStruct validationRequest,
    2: NoValidationStruct noValidationRequest
  )
  bool validateOnlyNonValidatedRequest (
    1: NoValidationStruct noValidationRequest
  )
  bool validateOnlyValidatedRequest (
    1: ValidationStruct validationRequest
  )
  bool validateNestedRequest (
   1: NestedNonValidationStruct nestedNonRequest
  )
  bool validateDeepNestedRequest (
   1: DeepNestedValidationstruct deepNestedRequest
  )
}
