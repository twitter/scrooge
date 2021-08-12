#@namespace scala com.twitter.scrooge.backend.thriftscala

struct ConstructorRequiredStruct {
  1: optional i64 optionalField
  2: required string requiredField
  3: optional i64 constructionRequiredField (construction_required = "true")
  4: i64 defaultRequirednessField
  # ensure that we can still call a field validateNewInstance.
  5: optional i64 validateNewInstance
}

struct DeepValidationStruct {
  1: required list<ConstructorRequiredStruct> inList
  2: required set<ConstructorRequiredStruct> inSet
  3: optional ConstructorRequiredStruct optionalConstructorRequiredStruct
  4: required ConstructorRequiredStruct requiredConstructorRequiredStruct
  5: required map<ConstructorRequiredStruct, string> inMapKey
  6: required map<string, ConstructorRequiredStruct> inMapValue
  7: required map<set<list<ConstructorRequiredStruct>>, set<list<ConstructorRequiredStruct>>> crazyEmbedding
  // test where the field is named the same as the type
  8: optional ConstructorRequiredStruct ConstructorRequiredStruct
}

union DeepValidationUnion {
  1: ConstructorRequiredStruct constructorRequiredStruct
  2: i64 otherField
}

struct ValidationStruct {
  1: string stringField (validation.length.min = "6", validation.email = "")
  2: i32 intField (validation.positiveOrZero = "")
  3: i64 longField (validation.max = "100")
  4: i16 shortField (validation.negative = "")
  5: i8 byteField (validation.positive = "")
  6: map<string, string> mapField (validation.size.max = "1")
  7: bool boolField (validation.assertTrue = "")
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
  1: i32 intField (validation.positiveOrZero = "")
  2: string stringField (validation.notEmpty = "")
}

exception ValidationException {
  1: string excField (validation.notEmpty = "")
}
