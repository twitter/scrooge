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
const set<string> someSimpleSet = ["foo", "bar"]
const list<string> anotherNoneEmptyList = ["kitty"]
const set<list<string>> someSet = [someList, anotherNoneEmptyList]


const list<string> nonEmptyPlainList = [name];

// TODO: CSL-364. Uncomment the following line once it's fixed.
// const list<list<string>> emptyNestedList = [emptyList];
// const set<list<string>> setWithEmptyListElement = [emptyList, someList]

const list<list<string>> nonEmptyNestedList = [nonEmptyPlainList];

// mixed cases for ids
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

