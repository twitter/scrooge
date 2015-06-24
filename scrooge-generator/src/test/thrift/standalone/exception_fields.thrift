exception MyException {}
struct MyStruct {}

struct ExceptionsAreStructs {
  1: MyException exceptionField
  2: MyStruct structField
  3: list<MyException> exceptionListField
  4: set<MyException> exceptionSetField
  5: map<string, MyException> stringToExceptionMapField
  6: map<MyException, string> exceptionToStringMapField
}

