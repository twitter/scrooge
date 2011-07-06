
- enums are not generated.

- handle optional/required [is there really anything to do there?]
x generate a struct for each args/return from a method
x handle "not present" fields in encoding (needed particularly for return-value structs)
- put constants in an object, not just loose in the file.



a field can be:
  - in a struct:
    + required
    + optional
    + (default) optional in, required out
  - in an arg list:
    + required
    + (default) optional in, required out

"in": when read from a stream via decoder
"out": when created via case class constructor

