package com.twitter.scrooge

import org.specs.Specification

class ScroogeParserSpec extends Specification {
  import AST._

  "ScroogeParser" should {
    val parser = new ScroogeParser()

    "constant" in {
      parser.parse("300", parser.constant) mustEqual IntConstant(300)
      parser.parse("300.5", parser.constant) mustEqual DoubleConstant(300.5)
      parser.parse("\"hello!\"", parser.constant) mustEqual StringConstant("hello!")
      parser.parse("'hello!'", parser.constant) mustEqual StringConstant("hello!")
      parser.parse("cat", parser.constant) mustEqual Identifier("cat")
      parser.parse("[ 4, 5 ]", parser.constant) mustEqual ListConstant(List(IntConstant(4),
        IntConstant(5)))
      parser.parse("{ 'name': 'Commie', 'home': 'San Francisco' }",
        parser.constant) mustEqual MapConstant(Map(StringConstant("name") -> StringConstant
        ("Commie"), StringConstant("home") -> StringConstant("San Francisco")))
    }

    "base types" in {
      parser.parse("i16", parser.fieldType) mustEqual TI16
      parser.parse("i32", parser.fieldType) mustEqual TI32
      parser.parse("i64", parser.fieldType) mustEqual TI64
      parser.parse("byte", parser.fieldType) mustEqual TByte
      parser.parse("double", parser.fieldType) mustEqual TDouble
      parser.parse("string", parser.fieldType) mustEqual TString
      parser.parse("bool", parser.fieldType) mustEqual TBool
      parser.parse("binary", parser.fieldType) mustEqual TBinary
    }

    "compound types" in {
      parser.parse("list<i64>", parser.fieldType) mustEqual ListType(TI64, None)
      parser.parse("list<list<string>>", parser.fieldType) mustEqual ListType(ListType(TString,
        None), None)
      parser.parse("map<string, list<bool>>", parser.fieldType) mustEqual MapType(TString,
        ListType(TBool, None), None)
      parser.parse("set<Monster>", parser.fieldType) mustEqual SetType(ReferenceType("Monster"),
        None)
      parser.parse("Monster", parser.fieldType) mustEqual ReferenceType("Monster")
    }

    "functions" in {
      parser.parse("void go()", parser.function) mustEqual Function("go", Void, Nil, false, Nil)
      parser.parse("oneway i32 double(1: i32 n)", parser.function) mustEqual Function("double",
        TI32, List(Field(1, "n", TI32, None, false)), true, Nil)
      parser.parse(
        "list<string> get_tables(optional i32 id, 3: string name = 'cat') throws (1: Exception ex);",
        parser.function) mustEqual
        Function("get_tables", ListType(TString, None), List(Field(0, "id", TI32, None, true),
        Field(3, "name", TString, Some(StringConstant("cat")), false)),
        false, List(Field(1, "ex", ReferenceType("Exception"), None, false)))
    }

  }
}
