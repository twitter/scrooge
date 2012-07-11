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
      parser.parse("# hello\n 300", parser.constant) mustEqual IntConstant(300)
    }

    "constant" in {
      parser.parse("300.5", parser.constant) mustEqual DoubleConstant(300.5)
      parser.parse("\"hello!\"", parser.constant) mustEqual StringConstant("hello!")
      parser.parse("'hello!'", parser.constant) mustEqual StringConstant("hello!")
      parser.parse("cat", parser.constant) mustEqual Identifier("cat")
      val list = parser.parse("[ 4, 5 ]", parser.constant)
      list must haveClass[ListConstant]
      list.asInstanceOf[ListConstant].elems.toList mustEqual List(IntConstant(4), IntConstant(5))
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
      parser.parse("/**doc!*/ void go()", parser.function) mustEqual
        Function("go", Void, Seq(), false, Seq(), Some("/**doc!*/"))
      parser.parse("/**one-way-docs*/ oneway i32 double(1: i32 n)", parser.function) mustEqual Function("double",
        TI32, Seq(Field(1, "n", TI32, None, Requiredness.Default)), true, Seq(), Some("/**one-way-docs*/"))
      parser.parse(
        "list<string> get_tables(optional i32 id, /**DOC*/3: required string name='cat') throws (1: Exception ex);",
        parser.function) mustEqual
        Function("get_tables", ListType(TString, None), Seq(
          Field(-1, "id", TI32, None, Requiredness.Optional),
          Field(3, "name", TString, Some(StringConstant("cat")), Requiredness.Required)
        ), false, Seq(Field(1, "ex", ReferenceType("Exception"), None, Requiredness.Default)), None)
    }

    "const" in {
      parser.parse("/** COMMENT */ const string name = \"Columbo\"", parser.definition) mustEqual Const("name",
        TString, StringConstant("Columbo"), Some("/** COMMENT */"))
    }

    "more than one docstring" in {
      val code = """
/** comment */
/** and another */
const string tyrion = "lannister"
"""
      parser.parse(code, parser.definition) mustEqual Const("tyrion",
        TString, StringConstant("lannister"), Some("/** comment */\n/** and another */"))
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
      parser.parse(code, parser.definition) mustEqual Enum("Direction", Seq(
        EnumValue("NORTH", 0),
        EnumValue("SOUTH", 1),
        EnumValue("EAST", 90),
        EnumValue("WEST", 91),
        EnumValue("UP", 92),
        EnumValue("DOWN", 5)
      ), None)

      parser.parse("enum Bad { a=1, b, c=2 }", parser.definition) must throwA[ParseException]

      val withComment = """
/**
 * Docstring!
 */
enum Foo
{
  X = 1,
  Y = 2
}"""
      parser.parse(withComment, parser.enum) mustEqual Enum("Foo",
        Seq(
          EnumValue("X", 1),
          EnumValue("Y", 2)),
        Some("/**\n * Docstring!\n */")
      )
    }


    "senum" in {
      // wtf is senum?!
      parser.parse("senum Cities { 'Milpitas', 'Mayfield' }", parser.definition) mustEqual
        Senum("Cities", Seq("Milpitas", "Mayfield"))
    }

    "struct" in {
      val code = """
        /** docs up here */
        struct Point {
          1: double x
          /** comments*/
          2: double y
          3: Color color = BLUE
        }
                 """
      parser.parse(code, parser.definition) mustEqual Struct("Point", Seq(
        Field(1, "x", TDouble, None, Requiredness.Default),
        Field(2, "y", TDouble, None, Requiredness.Default),
        Field(3, "color", ReferenceType("Color"), Some(Identifier("BLUE")), Requiredness.Default)
      ), Some("/** docs up here */"))
    }

    "exception" in {
      parser.parse("exception BadError { 1: string message }", parser.definition) mustEqual
        Exception_("BadError", Seq(Field(1, "message", TString, None, Requiredness.Default)), None)
      parser.parse("exception E { string message, string reason }", parser.definition) mustEqual
        Exception_("E", Seq(
          Field(-1, "message", TString, None, Requiredness.Default),
          Field(-2, "reason", TString, None, Requiredness.Default)
        ), None)
      parser.parse("exception NoParams { }", parser.definition) mustEqual
        Exception_("NoParams", Seq(), None)
      parser.parse("/** doc rivers */ exception wellDocumentedException { }", parser.definition) mustEqual
        Exception_("wellDocumentedException", Seq(), Some("/** doc rivers */"))
    }

    "service" in {
      val code = """
        /** cold hard cache */
        service Cache {
          void put(1: string name, 2: binary value);
          binary get(1: string name) throws (1: NotFoundException ex);
        }
                 """
      parser.parse(code, parser.definition) mustEqual Service("Cache", None, Seq(
        Function("put", Void, Seq(
          Field(1, "name", TString, None, Requiredness.Default),
          Field(2, "value", TBinary, None, Requiredness.Default)
        ), false, Seq(), None),
        Function("get", TBinary, Seq(
          Field(1, "name", TString, None, Requiredness.Default)
        ), false, Seq(Field(1, "ex", ReferenceType("NotFoundException"), None, Requiredness.Default)), None)
      ), Some("/** cold hard cache */"))

      parser.parse("service LeechCache extends Cache {}", parser.definition) mustEqual
        Service("LeechCache", Some(ServiceParent("Cache")), Seq(), None)
    }

    "document" in {
      val code = """
        namespace java com.example
        namespace * example

        /** what up doc */
        service NullService {
          /** DoC */
          void doNothing();
        }
                 """
      parser.parse(code, parser.document) mustEqual Document(
        Seq(Namespace("java", "com.example"), Namespace("*", "example")),
        Seq(Service("NullService", None, Seq(
          Function("doNothing", Void, Seq(), false, Seq(), Some("/** DoC */"))
        ), Some("/** what up doc */")))
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
