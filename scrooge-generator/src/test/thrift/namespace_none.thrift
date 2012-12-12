
enum Weekday
{
  MONDAY=1,
  TUESDAY,
  WEDNESDAY,
  THURSDAY,
  FRIDAY,
  SATURDAY=6,
  SUNDAY
}

// intentional name collision with Business service in namespace_single.thrift
struct Business {
  1: i32 numOfEmployees
}