package com.twitter.scrooge

import org.specs.Specification

class ScroogeParserSpec extends Specification {
  import AST._

  "ScroogeParser" should {
    val parser = new ScroogeParser(Importer.fakeImporter(Map.empty))

    "comments" in {
      parser.parse("  300  ", parser.constant) mustEqual IntConstant(300)
      parser.parse("  // go away.\n 300", parser.constant) mustEqual IntConstant(300)
      parser.parse("  /*\n   * go away.\n   */\n 300", parser.constant) mustEqual IntConstant(300)
    }

    "constant" in {
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
        "list<string> get_tables(optional i32 id, 3: string name='cat') throws (1: Exception ex);",
        parser.function) mustEqual
        Function("get_tables", ListType(TString, None), List(Field(0, "id", TI32, None, true),
        Field(3, "name", TString, Some(StringConstant("cat")), false)),
        false, List(Field(1, "ex", ReferenceType("Exception"), None, false)))
    }

    "const" in {
      parser.parse("const string name = \"Columbo\"", parser.definition) mustEqual Const("name",
        TString, StringConstant("Columbo"))
    }

    "typedef" in {
      parser.parse("typedef list<i32> Ladder", parser.definition) mustEqual Typedef("Ladder",
        ListType(TI32, None))
    }

    "enum" in {
      val code = """
        enum Direction {
          NORTH, SOUTH, EAST=90, WEST, UP, DOWN=5
        }
        """
      parser.parse(code, parser.definition) mustEqual Enum("Direction", List(
        EnumValue("NORTH", 1),
        EnumValue("SOUTH", 2),
        EnumValue("EAST", 90),
        EnumValue("WEST", 91),
        EnumValue("UP", 92),
        EnumValue("DOWN", 5)
      ))

      parser.parse("enum Bad { a=1, b, c=2 }", parser.definition) must throwA[ParseException]
    }

    "senum" in {
      // wtf is senum?!
      parser.parse("senum Cities { 'Milpitas', 'Mayfield' }", parser.definition) mustEqual
        Senum("Cities", List("Milpitas", "Mayfield"))
    }

    "struct" in {
      val code = """
        struct Point {
          1: double x
          2: double y
          3: Color color = BLUE
        }
        """
      parser.parse(code, parser.definition) mustEqual Struct("Point", List(
        Field(1, "x", TDouble, None, false),
        Field(2, "y", TDouble, None, false),
        Field(3, "color", ReferenceType("Color"), Some(Identifier("BLUE")), false)
      ))
    }

    "exception" in {
      parser.parse("exception BadError { 1: string message }", parser.definition) mustEqual
        Exception_("BadError", List(Field(1, "message", TString, None, false)))
    }

    "service" in {
      val code = """
        service Cache {
          void put(1: string name, 2: binary value);
          binary get(1: string name) throws (1: NotFoundException ex);
        }
        """
      parser.parse(code, parser.definition) mustEqual Service("Cache", None, List(
        Function("put", Void, List(
          Field(1, "name", TString, None, false),
          Field(2, "value", TBinary, None, false)
        ), false, Nil),
        Function("get", TBinary, List(
          Field(1, "name", TString, None, false)
        ), false, List(Field(1, "ex", ReferenceType("NotFoundException"), None, false)))
      ))

      parser.parse("service LeechCache extends Cache {}", parser.definition) mustEqual
        Service("LeechCache", Some("Cache"), Nil)
    }

    "document" in {
      val code = """
        namespace java com.example
        namespace * example

        service NullService {
          void doNothing();
        }
        """
      parser.parse(code, parser.document) mustEqual Document(
        List(Namespace("java", "com.example"), Namespace("*", "example")),
        List(Service("NullService", None, List(
          Function("doNothing", Void, Nil, false, Nil)
        )))
      )
    }

    "standard test file" in {
      val parser = new ScroogeParser(Importer.resourceImporter(getClass))
      val doc = parser.parseFile("/test.thrift")
      // i guess not blowing up is a good first-pass test.
      // might be nice to verify parts of it tho.
      doc.headers.size mustEqual 13
    }
  }
}
