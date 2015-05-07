namespace java thrift.test

const string name = "Columbo";
const string copyName = name;
/**
 I AM A DOC
 */
// not a doc, just a comment, but the doc should still get included.
const i32 someInt = 1;
const i64 someLong = 9223372036854775807;
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

const map<i64, i64> long_key_long_value_map = {
  1 : 1,
  2147483648 : 2147483648,
  -2147483649 : -2147483649
};

const set<i64> long_set = [ 1, 2147483648, -2147483649 ];

const list<i64> long_list = [ 1, 2147483648, -2147483649 ];

enum Color {
  RED = 1,
  BLUE = 2
}

const map<Color, string> colorNames = {
  Color.RED : "red",
  Color.BLUE : "blue"
}

const map<i32, map<Color, string>> mapOfMaps = {
  123 : colorNames
}

// mixed cases for ids
enum weekDay {
  // not a doc, just a comment.
  mon = 1,
  // not a doc, just a comment.
  TUE,
  /** I am a doc. */
  // not a doc, just a comment.
  Wed = 3,
  /** I am a doc. */
  thu,
  Fri,
  Sat,
  sUN
}

const weekDay myWfhDay = weekDay.thu;
const list<weekDay> myDaysOut = [myWfhDay, weekDay.Sat, weekDay.sUN]

