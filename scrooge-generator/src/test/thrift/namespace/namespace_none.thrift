
enum Weekday
{
  Monday=1,
  Tuesday,
  Wednesday,
  Thursday,
  Friday,
  Saturday=6,
  Sunday
}

// intentional name collision with Business service in namespace_single.thrift
struct Business {
  1: i32 numOfEmployees
}