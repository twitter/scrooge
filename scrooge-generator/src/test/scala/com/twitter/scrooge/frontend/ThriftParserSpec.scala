package com.twitter.scrooge.frontend

import com.twitter.scrooge.ast._
import org.specs.SpecificationWithJUnit
import com.twitter.scrooge._

class ThriftParserSpec extends SpecificationWithJUnit {
  "ThriftParser" should {
    val parser = new ThriftParser(NullImporter, true)

    "comments" in {
      parser.parse("  300  ", parser.rhs) mustEqual IntLiteral(300)
      parser.parse("  // go away.\n 300", parser.rhs) mustEqual IntLiteral(300)
      parser.parse("  /*\n   * go away.\n   */\n 300", parser.rhs) mustEqual IntLiteral(300)
      parser.parse("# hello\n 300", parser.rhs) mustEqual IntLiteral(300)
    }

    "constant" in {
      parser.parse("300.5", parser.rhs) mustEqual DoubleLiteral(300.5)
      parser.parse("\"hello!\"", parser.rhs) mustEqual StringLiteral("hello!")
      parser.parse("'hello!'", parser.rhs) mustEqual StringLiteral("hello!")
      parser.parse("cat", parser.rhs) mustEqual IdRHS(SimpleID("cat"))
      val list = parser.parse("[ 4, 5 ]", parser.rhs)
      list must haveClass[ListRHS]
      list.asInstanceOf[ListRHS].elems.toList mustEqual List(IntLiteral(4), IntLiteral(5))
      parser.parse("{ 'name': 'Commie', 'home': 'San Francisco' }",
        parser.rhs) mustEqual MapRHS(Map(StringLiteral("name") -> StringLiteral
        ("Commie"), StringLiteral("home") -> StringLiteral("San Francisco")))
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
      parser.parse("set<Monster>", parser.fieldType) mustEqual SetType(ReferenceType(Identifier("Monster")),
        None)
      parser.parse("Monster", parser.fieldType) mustEqual ReferenceType(Identifier("Monster"))
    }

    "functions" in {
      parser.parse("/**doc!*/ void go()", parser.function) mustEqual
        Function(SimpleID("go"), "go", Void, Seq(), Seq(), Some("/**doc!*/"))
      parser.parse(
        "list<string> get_tables(optional i32 id, /**DOC*/3: required string name='cat') throws (1: Exception ex);",
        parser.function) mustEqual
        Function(SimpleID("get_tables"), "get_tables", ListType(TString, None), Seq(
          Field(-1, SimpleID("id"), "id", TI32, None, Requiredness.Optional),
          Field(3, SimpleID("name"), "name", TString, Some(StringLiteral("cat")), Requiredness.Required)
        ), Seq(Field(1, SimpleID("ex"), "ex", ReferenceType(Identifier("Exception")), None, Requiredness.Default)), None)
    }

    "const" in {
      parser.parse("/** COMMENT */ const string name = \"Columbo\"", parser.definition) mustEqual ConstDefinition(SimpleID("name"),
        TString, StringLiteral("Columbo"), Some("/** COMMENT */"))
    }

    "more than one docstring" in {
      val code = """
/** comment */
/** and another */
const string tyrion = "lannister"
"""
      parser.parse(code, parser.definition) mustEqual ConstDefinition(SimpleID("tyrion"),
        TString, StringLiteral("lannister"), Some("/** comment */\n/** and another */"))
    }

    "typedef" in {
      parser.parse("typedef list<i32> Ladder", parser.definition) mustEqual Typedef(SimpleID("Ladder"),
        ListType(TI32, None))
    }

    "enum" in {
      val code = """
        enum Direction {
          NORTH, SOUTH, EAST=90, WEST, UP, DOWN=5
        }
        """
      parser.parse(code, parser.definition) mustEqual Enum(SimpleID("Direction"), Seq(
        EnumField(SimpleID("NORTH"), 0, None),
        EnumField(SimpleID("SOUTH"), 1, None),
        EnumField(SimpleID("EAST"), 90, None),
        EnumField(SimpleID("WEST"), 91, None),
        EnumField(SimpleID("UP"), 92, None),
        EnumField(SimpleID("DOWN"), 5, None)
      ), None)

      val withComment = """
/**
 * Docstring!
 */
enum Foo
{
  /** I am a doc. */
  // I am a comment.
  X = 1,
  // I am a comment.
  Y = 2
}"""
      parser.parse(withComment, parser.enum) mustEqual Enum(SimpleID("Foo"),
        Seq(
          EnumField(SimpleID("X"), 1, Some("/** I am a doc. */")),
          EnumField(SimpleID("Y"), 2, None)),
        Some("/**\n * Docstring!\n */")
      )
    }


    "senum" in {
      // wtf is senum?!
      parser.parse("senum Cities { 'Milpitas', 'Mayfield' }", parser.definition) mustEqual
        Senum(SimpleID("Cities"), Seq("Milpitas", "Mayfield"))
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
      parser.parse(code, parser.definition) mustEqual Struct(SimpleID("Point"), "Point", Seq(
        Field(1, SimpleID("x"), "x", TDouble, None, Requiredness.Default),
        Field(2, SimpleID("y"), "y", TDouble, None, Requiredness.Default),
        Field(3, SimpleID("color"), "color", ReferenceType(Identifier("Color")), Some(IdRHS(SimpleID("BLUE"))), Requiredness.Default)
      ), Some("/** docs up here */"))
    }

    "union" in {
      "basic" in {
        val code = """
          /** docs up here */
          union Aircraft {
            1: Airplane a
            /** comments*/
            2: Rotorcraft r
            3: Glider g
            4: LighterThanAir lta
          }
                   """
        parser.parse(code, parser.definition) mustEqual Union(SimpleID("Aircraft"), "Aircraft", Seq(
          Field(1, SimpleID("a"), "a", ReferenceType(Identifier("Airplane")), None, Requiredness.Default),
          Field(2, SimpleID("r"), "r", ReferenceType(Identifier("Rotorcraft")), None, Requiredness.Default),
          Field(3, SimpleID("g"), "g", ReferenceType(Identifier("Glider")), None, Requiredness.Default),
          Field(4, SimpleID("lta"), "lta", ReferenceType(Identifier("LighterThanAir")), None, Requiredness.Default)
        ), Some("/** docs up here */"))
      }

      "requiredness" in {
        parser.parse("union Aircraft { 1: required Airplane a }", parser.definition) must
          throwA[UnionFieldRequiredException]
        parser.parse("union Aircraft { 1: optional Airplane a }", parser.definition) must
          throwA[UnionFieldOptionalException]

        val laxParser = new ThriftParser(NullImporter, false)
        val code = """
          union Aircraft {
            1: required Airplane a
            2: optional Rotorcraft r
            3: Glider g
          }
                   """

        laxParser.parse(code, laxParser.definition) mustEqual Union(SimpleID("Aircraft"), "Aircraft", Seq(
          Field(1, SimpleID("a"), "a", ReferenceType(Identifier("Airplane")), None, Requiredness.Default),
          Field(2, SimpleID("r"), "r", ReferenceType(Identifier("Rotorcraft")), None, Requiredness.Default),
          Field(3, SimpleID("g"), "g", ReferenceType(Identifier("Glider")), None, Requiredness.Default)
        ), None)
      }
    }

    "exception" in {
      parser.parse("exception BadError { 1: string message }", parser.definition) mustEqual
        Exception_(SimpleID("BadError"), "BadError",
          Seq(Field(1, SimpleID("message"), "message", TString, None, Requiredness.Default)), None)
      parser.parse("exception E { string message, string reason }", parser.definition) mustEqual
        Exception_(SimpleID("E"), "E", Seq(
          Field(-1, SimpleID("message"), "message", TString, None, Requiredness.Default),
          Field(-2, SimpleID("reason"), "reason", TString, None, Requiredness.Default)
        ), None)
      parser.parse("exception NoParams { }", parser.definition) mustEqual
        Exception_(SimpleID("NoParams"), "NoParams", Seq(), None)
      parser.parse("/** doc rivers */ exception wellDocumentedException { }", parser.definition) mustEqual
        Exception_(SimpleID("wellDocumentedException"), "wellDocumentedException", Seq(), Some("/** doc rivers */"))
    }

    "service" in {
      val code = """
        /** cold hard cache */
        service Cache {
          void put(1: string name, 2: binary value);
          binary get(1: string name) throws (1: NotFoundException ex);
        }
                 """
      parser.parse(code, parser.definition) mustEqual Service(SimpleID("Cache"), None, Seq(
        Function(SimpleID("put"), "put", Void, Seq(
          Field(1, SimpleID("name"), "name", TString, None, Requiredness.Default),
          Field(2, SimpleID("value"), "value", TBinary, None, Requiredness.Default)
        ), Seq(), None),
        Function(SimpleID("get"), "get", TBinary, Seq(
          Field(1, SimpleID("name"), "name", TString, None, Requiredness.Default)
        ), Seq(Field(1, SimpleID("ex"), "ex", ReferenceType(Identifier("NotFoundException")), None, Requiredness.Default)), None)
      ), Some("/** cold hard cache */"))

      parser.parse("service LeechCache extends Cache {}", parser.definition) mustEqual
        Service(
          SimpleID("LeechCache"),
          Some(ServiceParent(SimpleID("Cache"), None)),
          Seq(),
          None)
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
        Seq(Namespace("java", Identifier("com.example")), Namespace("*", Identifier("example"))),
        Seq(Service(SimpleID("NullService"), None, Seq(
          Function(SimpleID("doNothing"), "doNothing", Void, Seq(), Seq(), Some("/** DoC */"))
        ), Some("/** what up doc */")))
      )
    }

    // reject syntax
    "reject oneway modifier" in {
      parser.parse("/**one-way-docs*/ oneway i32 double(1: i32 n)", parser.function) must
        throwA[OnewayNotSupportedException]
    }

    "ignore oneway modifier with strict mode off" in {
      val parserNonStrict = new ThriftParser(NullImporter, false)
      parserNonStrict.parse("/**one-way-docs*/ oneway i32 double(1: i32 n)", parserNonStrict.function) mustEqual
        Function(SimpleID("double"), "double", TI32, Seq(
          Field(1, SimpleID("n"), "n", TI32)
        ), Seq(), Some("/**one-way-docs*/"))
    }

    "reject negative field ids" in {
      val code =
        """
          struct Point {
            1: double x
            -2: double y
            3: Color color = BLUE
          }
        """
      parser.parse(code, parser.definition) must throwA[NegativeFieldIdException]
    }

    "reject duplicate field ids" in {
      val code =
        """
          struct Point {
            1: double x
            2: double y
            2: Color color = BLUE
          }
        """
      parser.parse(code, parser.definition) must throwA[DuplicateFieldIdException]
    }

    "reject duplicate enum values" in {
      parser.parse("enum Bad { a=1, b, c=2 }", parser.definition) must throwA[RepeatingEnumValueException]

      val code = """
        enum Direction {
          NORTH, SOUTH, EAST=90, WEST=90, UP, DOWN=5
        }
      """
      parser.parse(code, parser.definition) must throwA[RepeatingEnumValueException]
    }

    "ignore annotations" in {
      parser.parse("""typedef string (dbtype="fixedchar(4)", nullable="false") AirportCode""",
        parser.definition) mustEqual Typedef(SimpleID("AirportCode"), TString)

      val code =
        """
          struct Airport {
            1: optional i64 id(autoincrement="true"),
            2: optional string(dbtype="varchar(255)") code,
            3: optional string name
          } (primary_key="(id)",
             index="code_idx(code)",
             sql_name="airports",)
        """
      parser.parse(code, parser.definition) mustEqual Struct(SimpleID("Airport"), "Airport", Seq(
        Field(1, SimpleID("id"), "id", TI64, None, Requiredness.Optional),
        Field(2, SimpleID("code"), "code", TString, None, Requiredness.Optional),
        Field(3, SimpleID("name"), "name", TString, None, Requiredness.Optional)
      ), None)
    }
  }
}
