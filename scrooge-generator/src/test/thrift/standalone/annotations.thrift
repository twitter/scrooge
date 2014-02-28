namespace java scrooge.test.annotations.thriftjava
#@namespace scala scrooge.test.annotations.thriftscala

# The data types in this file exercise Thrift annotations in all legal positions.

struct AnnoStruct {
  1: string (structTypeKey = "structTypeValue") structField (structFieldKey = "structFieldValue")
} (structKey = "structValue")

# This version of AnnoStruct exercises multiple annotations.
struct MultiAnnoStruct {
  1: string (structTypeKey1 = "structTypeValue1", structTypeKey2 = "structTypeValue2") multiStructField (structFieldKey1 = "structFieldValue1", structFieldKey2 = "structFieldValue2")
} (structKey1 = "structValue1", structKey2 = "structValue2")

union AnnoUnion {
  1: AnnoStruct unionField (unionFieldKey = "unionFieldValue")
} (unionKey = "unionValue")

exception AnnoException {
  1: string (excTypeKey = "excTypeValue") excField (excFieldKey = "excFieldValue")
}

service AnnoService {
  AnnoStruct method(
    1: string (methodFieldTypeKey = "methodFieldTypeValue") field (methodFieldFieldKey = "methodFieldFieldValue")
  ) throws (1: AnnoException exc (methodExcKey = "methodExcValue"))
}
