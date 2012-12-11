namespace java bar // todo CSL-322, CSL-338: get rid of this line after implementing default namespace

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