namespace java thrift.test

const string name = "Columbo";
const string copyName = name;
/**
 I AM A DOC
 */
// not a doc, just a comment, but the doc should still get included.
const i32 someInt = 1;
const double someDouble = 3.0;
const list<string> someList = ["piggy"];
const list<string> emptyList = [];
const map<string,string> someMap = {"foo": "bar"};

const list<string> nonEmptyPlainList = [name];

// TODO: CSL-364. Uncomment the following line once it's fixed.
// const list<list<string>> emptyNestedList = [emptyList];

const list<list<string>> nonEmptyNestedList = [nonEmptyPlainList];

enum Weekday {
  MON = 1,
  TUE,
  WED = 3,
  THU,
  FRI,
  SAT,
  SUN
}

const Weekday myWfhDay = Weekday.THU;

// todo: CSL-365. Uncomment the following line once it's fixed.
// const list<Weekday> myDaysOut = [myWfhday, Weekday.SAT, Weekday.SUN]

