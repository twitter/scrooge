#@namespace scala com.twitter.scrooge.adapt.thrift

struct TestStruct {
  1: required bool boolField;
  2: required byte byteField;
  3: required i16 shortField;
  4: required i32 intField;
  5: required i64 longField;
  6: required double doubleField;
  7: required string stringField;
  8: required binary binaryField;
  9: optional bool optionalField;
  10: required list<bool> listField;
  11: required set<bool> setField;
  12: required map<bool,bool> mapField;
  13: required i64 annotatedId (test.space = "User", test.name = "annotated" );
  14: required string type;
  15: optional string class;
  16: optional string optionalField2;
  17: optional string optionalFieldWithDefaultValue = "default_value";

  required bool negativeField;
  required bool snake_case;
  required bool EndOffset;
}

struct TestNestedStruct {
  1: required TestStruct field;
  2: required TestStruct type;
  3: optional TestStruct class;
  4: optional TestStruct optionalField;
  5: required list<TestStruct> seqField;
  6: required set<TestStruct> setField;
  7: required map<TestStruct,TestStruct> mapField;
}(test.idl = "true")

union TestUnion {
  1: bool boolArm;
  2: string stringArm;
}

enum TestEnum { FOO = 0, BAR = 1 }

struct TestDefaultsStruct {
  1: required bool boolField;
  2: required i16 shortField = 7;
  3: optional i32 intField = 11;
}

struct TestRequiredDefaultsStruct {
  1: required string stringField = "NOPE";
  2: required list<string> listField;
}

struct TestEmptyStruct {}

struct TestOptionalFieldNoDefault {
  1: bool boolField;
  2: optional i32 intField; // explicitly no default value here
}

union TestEmptyStructUnion {
  1: TestEmptyStruct firstArm;
  2: TestEmptyStruct secondArm;
}

union TestStructUnion {
  1: TestStruct first;
  2: TestStruct second;
}

struct TestHasEmptyStructUnion {
  1: TestEmptyStructUnion unionField;
  2: optional string stringField;
  3: required i32 intField;
}

struct TestMissingField1 {
  1: i64 longField;
  2: bool boolField;
}

struct TestMissingField2 {
  1: i64 longField;
  2: bool boolField;
  3: i32 intField;
}

struct TestRequiredField {
  1: required bool requiredField;
  2: optional string optionalField;
}

struct TestPassthroughFields {
  1: required string passthroughFields;
}
