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
