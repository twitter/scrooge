Implementation Semantics
========================

Thrift is severely underspecified with respect to the handling of
required/optional/unspecified-requiredness and default values in various cases
such as serialization, deserialization, and new instance creation, and
different implementations do different things (see
http://lionet.livejournal.com/66899.html for a good analysis).

Scrooge attempts to be as rigorous as possible in this regard with
consistently applied and hopefully easy to understand rules.

1. If neither "required" nor "optional" is declared for a field, it then has
   the default requiredness of "optional-in/required-out", or "optInReqOut"
   for short.

2. It is invalid for a required field to be null and an exception will be
   thrown if you attempt to serialize a struct with a null required field.

3. It is invalid for a required field to be missing during deserialization,
   and an exception will be thrown in this case.

4. Optional fields can be set or unset, and the set-state is meaningful state
   of the struct that should be preserved by serialization/deserialization.
   Un-set fields are not present in the serialized representation of the
   struct.

5. Declared default values will be assigned to any non-required fields that
   are missing during deserialization. If no default is declared for a field,
   a default value appropriate for the type will be used (see below).

6. #4 and #5 imply that optional-with-default-value is not a tenable
   combination, and will be treated as if "optional" was not specified
   (optInReqOut-with-default-value).

Default values by type
----------------------

- bool = false
- byte/i16/i32/i64/double = 0
- string/struct/enum = null
- list = Seq()
- set = Set()
- map = Map()

Missing values
--------------

The following "matrix" defines all the scenarios where a value may not be
present and how that case is handled:

**required, no declared default value:**

- missing in deserialization:
    - throws TProtocolException
- null in serialization:
    - throws TProtocolException
- immutable instance instantiation:
    - must be explicitly provided

**required, with declared default value:**

- missing in deserialization:
    - throws TProtocolException
- null in serialization:
    - throws TProtocolException
- immutable instance instantiation:
    - declared default value

**optInReqOut, no declared default value:**

- missing in deserialization:
    - default value for type
- null in serialization:
    - throws TProtocolException
- immutable instance instantiation:
    - must be explicitly provided

**optInReqOut, with declared default value:**

- missing in deserialization:
    - declared default value
- null in serialization:
    - throws TProtocolException
- immutable instance instantiation:
    - declared default value

**optional, no declared default value:**

- missing in deserialization:
    - None
- None in serialization:
    - omitted
- immutable instance instantiation:
    - None

**optional, with declared default value:**

- case not valid, treated as optInReqOut with declared default value
