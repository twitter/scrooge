namespace java com.twitter.example.thrift

enum weekDay {
  mon = 1,
  TUE,
  Wed = 3,
  thu,
  Fri,
  Sat,
  sUN
}

const weekDay myWfhDay = weekDay.thu;
const list<weekDay> myDaysOut = [myWfhDay, weekDay.Sat, weekDay.sUN]


