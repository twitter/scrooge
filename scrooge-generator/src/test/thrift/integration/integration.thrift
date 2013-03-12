namespace java com.twitter.scrooge.integration_scala

// intentionally crooked letter cases

service FOOservice {
  void bar_method(1: string bar_arg)
  i32 baz_function(1: bool baz_arg1, 2: i32 bazARG2)
}

struct Bonk_struct
{
  1: string message,
  2: i32 int_thing
}

union bonk_or_bool_union {
  1: Bonk_struct bonk
  2: bool bool_thing
}