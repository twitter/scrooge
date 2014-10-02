package com.twitter.scrooge.frontend

import com.twitter.scrooge._
import com.twitter.scrooge.ast._
import com.twitter.scrooge.testutil.Spec

class ThriftParserSpec extends Spec {
  "ThriftParser" should {
    val parser = new ThriftParser(NullImporter, true)

    "comments" in {
      parser.parse("  300  ", parser.rhs) must be( IntLiteral(300))
      parser.parse("  // go away.\n 300", parser.rhs) must be( IntLiteral(300))
      parser.parse("  /*\n   * go away.\n   */\n 300", parser.rhs) must be(IntLiteral(300))
      parser.parse("# hello\n 300", parser.rhs) must be(IntLiteral(300))
    }

    "double-quoted strings" in {
      parser.parse(""" "hello!" """, parser.rhs) must be(StringLiteral("hello!"))
      parser.parse(""" "hello\nthere!" """, parser.rhs) must be(StringLiteral("""hello\nthere!"""))
      parser.parse(""" "hello\\nthere!" """, parser.rhs) must be(StringLiteral("""hello\\nthere!"""))
      parser.parse(""" "hello//there!" """, parser.rhs) must be(StringLiteral("""hello//there!"""))
      parser.parse(""" "hello'there!" """, parser.rhs) must be(StringLiteral("""hello'there!"""))
      parser.parse(""" "hello\'there!" """, parser.rhs) must be(StringLiteral("""hello\'there!"""))
      parser.parse(""" "hello\"there!" """, parser.rhs) must be(StringLiteral("""hello\"there!"""))
      parser.parse(""" "\"" """, parser.rhs) must be(StringLiteral("\\\""))
    }

    "single-quoted strings" in {
      parser.parse(""" 'hello!' """, parser.rhs) must be(StringLiteral("hello!"))
      parser.parse(""" 'hello\nthere!' """, parser.rhs) must be(StringLiteral("""hello\nthere!"""))
      parser.parse(""" 'hello\\nthere!' """, parser.rhs) must be(StringLiteral("""hello\\nthere!"""))
      parser.parse(""" 'hello//there!' """, parser.rhs) must be(StringLiteral("""hello//there!"""))
      parser.parse(""" 'hello"there!' """, parser.rhs) must be(StringLiteral("""hello"there!"""))
      parser.parse(""" 'hello\"there!' """, parser.rhs) must be(StringLiteral("""hello\"there!"""))
      parser.parse(""" 'hello\'there!' """, parser.rhs) must be(StringLiteral("""hello\'there!"""))
      parser.parse(""" '\'' """, parser.rhs) must be(StringLiteral("\\'"))
    }

    "constant" in {
      parser.parse("300.5", parser.rhs) must be(DoubleLiteral(300.5))
      parser.parse("cat", parser.rhs) must be(IdRHS(SimpleID("cat")))
      val list = parser.parse("[ 4, 5, ]", parser.rhs)
      list.isInstanceOf[ListRHS] must be(true)
      list.asInstanceOf[ListRHS].elems.toList must be(List(IntLiteral(4), IntLiteral(5)))
      parser.parse("{ 'name': 'Commie', 'home': 'San Francisco', }",
        parser.rhs) must be(MapRHS(Seq(StringLiteral("name") -> StringLiteral
        ("Commie"), StringLiteral("home") -> StringLiteral("San Francisco"))))
    }

    "base types" in {
      parser.parse("i16", parser.fieldType) must be(TI16)
      parser.parse("i32", parser.fieldType) must be(TI32)
      parser.parse("i64", parser.fieldType) must be(TI64)
      parser.parse("byte", parser.fieldType) must be(TByte)
      parser.parse("double", parser.fieldType) must be(TDouble)
      parser.parse("string", parser.fieldType) must be(TString)
      parser.parse("bool", parser.fieldType) must be(TBool)
      parser.parse("binary", parser.fieldType) must be(TBinary)
    }

    "compound types" in {
      parser.parse("list<i64>", parser.fieldType) must be(ListType(TI64, None))
      parser.parse("list<list<string>>", parser.fieldType) must be(ListType(ListType(TString,
        None), None))
      parser.parse("map<string, list<bool>>", parser.fieldType) must be(MapType(TString,
        ListType(TBool, None), None))
      parser.parse("set<Monster>", parser.fieldType) must be(SetType(ReferenceType(Identifier("Monster")),
        None))
      parser.parse("Monster", parser.fieldType) must be(ReferenceType(Identifier("Monster")))
    }

    "functions" in {
      parser.parse("/**doc!*/ void go()", parser.function) must be(
        Function(SimpleID("go"), "go", Void, Seq(), Seq(), Some("/**doc!*/")))
      parser.parse(
        "list<string> get_tables(optional i32 id, /**DOC*/3: required string name='cat') throws (1: Exception ex);",
        parser.function) must be(
        Function(SimpleID("get_tables"), "get_tables", ListType(TString, None), Seq(
          Field(-1, SimpleID("id"), "id", TI32, None, Requiredness.Optional),
          Field(3, SimpleID("name"), "name", TString, Some(StringLiteral("cat")), Requiredness.Required)
        ), Seq(Field(1, SimpleID("ex"), "ex", ReferenceType(Identifier("Exception")), None, Requiredness.Default)), None))
    }

    "const" in {
      parser.parse("/** COMMENT */ const string name = \"Columbo\"", parser.definition) must be(ConstDefinition(SimpleID("name"),
        TString, StringLiteral("Columbo"), Some("/** COMMENT */")))
    }

    "more than one docstring" in {
      val code = """
/** comment */
/** and another */
const string tyrion = "lannister"
"""
      parser.parse(code, parser.definition) must be(ConstDefinition(SimpleID("tyrion"),
        TString, StringLiteral("lannister"), Some("/** comment */\n/** and another */")))
    }

    "typedef" in {
      parser.parse(
        """typedef list<i32> (information="important", more="better") Ladder""",
        parser.definition
      ) must be(
        Typedef(
          SimpleID("Ladder"),
          ListType(TI32, None),
          Map("information" -> "important", "more" -> "better")
        ))
    }

    "enum" in {
      val code = """
        enum Direction {
          NORTH, SOUTH, EAST=90, WEST, UP, DOWN=5
        }
        """
      parser.parse(code, parser.definition) must be(Enum(SimpleID("Direction"), Seq(
        EnumField(SimpleID("NORTH"), 0, None),
        EnumField(SimpleID("SOUTH"), 1, None),
        EnumField(SimpleID("EAST"), 90, None),
        EnumField(SimpleID("WEST"), 91, None),
        EnumField(SimpleID("UP"), 92, None),
        EnumField(SimpleID("DOWN"), 5, None)
      ), None))

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
      parser.parse(withComment, parser.enum) must be(Enum(SimpleID("Foo"),
        Seq(
          EnumField(SimpleID("X"), 1, Some("/** I am a doc. */")),
          EnumField(SimpleID("Y"), 2, None)),
        Some("/**\n * Docstring!\n */")
      ))
    }


    "senum" in {
      // wtf is senum?!
      parser.parse("senum Cities { 'Milpitas', 'Mayfield' }", parser.definition) must be(
        Senum(SimpleID("Cities"), Seq("Milpitas", "Mayfield")))
    }

    "struct" in {
      val code = """
        /** docs up here */
        struct Point {
          1: double x
          /** comments*/
          2: double y
          3: Color color = BLUE
        } (
          annotation="supported",
          multiline="also supported",
        )
                 """
      parser.parse(code, parser.definition) must be(Struct(SimpleID("Point"), "Point", Seq(
        Field(1, SimpleID("x"), "x", TDouble, None, Requiredness.Default),
        Field(2, SimpleID("y"), "y", TDouble, None, Requiredness.Default),
        Field(3, SimpleID("color"), "color", ReferenceType(Identifier("Color")), Some(IdRHS(SimpleID("BLUE"))), Requiredness.Default)
      ), Some("/** docs up here */"), Map("annotation" -> "supported", "multiline" -> "also supported")))
    }

    "union" should {
      "basic" in {
        val code = """
          /** docs up here */
          union Aircraft {
            1: Airplane a
            /** comments*/
            2: Rotorcraft r
            3: Glider g
            4: LighterThanAir lta
          } (maxTypes="4")
                   """
        parser.parse(code, parser.definition) must be(Union(SimpleID("Aircraft"), "Aircraft", Seq(
          Field(1, SimpleID("a"), "a", ReferenceType(Identifier("Airplane")), None, Requiredness.Default),
          Field(2, SimpleID("r"), "r", ReferenceType(Identifier("Rotorcraft")), None, Requiredness.Default),
          Field(3, SimpleID("g"), "g", ReferenceType(Identifier("Glider")), None, Requiredness.Default),
          Field(4, SimpleID("lta"), "lta", ReferenceType(Identifier("LighterThanAir")), None, Requiredness.Default)
        ), Some("/** docs up here */"), Map("maxTypes" -> "4")))
      }

      "requiredness" in {
        intercept[UnionFieldRequiredException] {
          parser.parse("union Aircraft { 1: required Airplane a }", parser.definition)
        }
        intercept[UnionFieldOptionalException] {
          parser.parse("union Aircraft { 1: optional Airplane a }", parser.definition)
        }

        val laxParser = new ThriftParser(NullImporter, false)
        val code = """
          union Aircraft {
            1: required Airplane a
            2: optional Rotorcraft r
            3: Glider g
          }
                   """

        laxParser.parse(code, laxParser.definition) must be(Union(SimpleID("Aircraft"), "Aircraft", Seq(
          Field(1, SimpleID("a"), "a", ReferenceType(Identifier("Airplane")), None, Requiredness.Default),
          Field(2, SimpleID("r"), "r", ReferenceType(Identifier("Rotorcraft")), None, Requiredness.Default),
          Field(3, SimpleID("g"), "g", ReferenceType(Identifier("Glider")), None, Requiredness.Default)
        ), None, Map.empty))
      }

      "invalid field name" in {
        intercept[UnionFieldInvalidNameException] {
          parser.parse("""
            union Fruit {
              1: Apple apple
              2: Banana banana
              3: UnknownFruit unknown_union_field
            }
          """, parser.definition)
        }
      }
    }

    "exception" in {
      parser.parse("exception BadError { 1: string message }", parser.definition) must be(
        Exception_(SimpleID("BadError"), "BadError",
          Seq(Field(1, SimpleID("message"), "message", TString, None, Requiredness.Default)), None))
      parser.parse("exception E { string message, string reason }", parser.definition) must be(
        Exception_(SimpleID("E"), "E", Seq(
          Field(-1, SimpleID("message"), "message", TString, None, Requiredness.Default),
          Field(-2, SimpleID("reason"), "reason", TString, None, Requiredness.Default)
        ), None))
      parser.parse("exception NoParams { }", parser.definition) must be(
        Exception_(SimpleID("NoParams"), "NoParams", Seq(), None))
      parser.parse("/** doc rivers */ exception wellDocumentedException { }", parser.definition) must be(
        Exception_(SimpleID("wellDocumentedException"), "wellDocumentedException", Seq(), Some("/** doc rivers */")))
    }

    "service" in {
      val code = """
        /** cold hard cache */
        service Cache {
          void put(1: string name, 2: binary value);
          binary get(1: string name) throws (1: NotFoundException ex);
        }
                 """
      parser.parse(code, parser.definition) must be(Service(SimpleID("Cache"), None, Seq(
        Function(SimpleID("put"), "put", Void, Seq(
          Field(1, SimpleID("name"), "name", TString, None, Requiredness.Default),
          Field(2, SimpleID("value"), "value", TBinary, None, Requiredness.Default)
        ), Seq(), None),
        Function(SimpleID("get"), "get", TBinary, Seq(
          Field(1, SimpleID("name"), "name", TString, None, Requiredness.Default)
        ), Seq(Field(1, SimpleID("ex"), "ex", ReferenceType(Identifier("NotFoundException")), None, Requiredness.Default)), None)
      ), Some("/** cold hard cache */")))

      parser.parse("service LeechCache extends Cache {}", parser.definition) must be(
        Service(
          SimpleID("LeechCache"),
          Some(ServiceParent(SimpleID("Cache"), None)),
          Seq(),
          None))
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
      parser.parse(code, parser.document) must be(Document(
        Seq(Namespace("java", Identifier("com.example")), Namespace("*", Identifier("example"))),
        Seq(Service(SimpleID("NullService"), None, Seq(
          Function(SimpleID("doNothing"), "doNothing", Void, Seq(), Seq(), Some("/** DoC */"))
        ), Some("/** what up doc */")))
      ))
    }

    // reject syntax

    "reject negative field ids" in {
      val code =
        """
          struct Point {
            1: double x
            -2: double y
            3: Color color = BLUE
          }
        """
      intercept[NegativeFieldIdException] {
        parser.parse(code, parser.definition)
      }
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
      intercept[DuplicateFieldIdException] {
        parser.parse(code, parser.definition)
      }
    }

    "reject duplicate enum values" in {
      intercept[RepeatingEnumValueException] {
        parser.parse("enum Bad { a=1, b, c=2 }", parser.definition)
      }

      val code = """
        enum Direction {
          NORTH, SOUTH, EAST=90, WEST=90, UP, DOWN=5
        }
      """
      intercept[RepeatingEnumValueException] {
        parser.parse(code, parser.definition)
      }
    }

    "handle struct annotations" in {
      parser.parse(
        """typedef string (dbtype="fixedchar(4)", nullable="false") AirportCode""",
        parser.definition
      ) must be(
        Typedef(
          SimpleID("AirportCode"),
          TString,
          Map("dbtype" -> "fixedchar(4)", "nullable" -> "false")
        ))

      val idTypeAnnotations = Map("autoincrement" -> "true")
      val idFieldAnnotations = Map("initialValue" -> "0")
      val codeTypeAnnotations = Map("dbtype" -> "varchar(255)")
      val nameFieldAnnotations = Map("postid" -> "varchar(255)")
      val structAnnotations = Map(
        "primary_key" -> "(id)",
        "index" -> "code_idx(code)",
        "sql_name" -> "airports"
      )
      val code =
        """
          struct Airport {
            1: optional i64 (autoincrement="true") id = 0(initialValue="0"),
            2: optional string(dbtype="varchar(255)") code,
            3: optional string name(postid="varchar(255)")
          } (primary_key="(id)",
             index="code_idx(code)",
             sql_name="airports",)
        """
      parser.parse(code, parser.definition) must be(
        Struct(
          SimpleID("Airport"),
          "Airport",
          Seq(
            Field(1, SimpleID("id"), "id", TI64, Some(IntLiteral(0)), Requiredness.Default, idTypeAnnotations, idFieldAnnotations),
            Field(2, SimpleID("code"), "code", TString, None, Requiredness.Optional, codeTypeAnnotations, Map.empty),
            Field(3, SimpleID("name"), "name", TString, None, Requiredness.Optional, Map.empty, nameFieldAnnotations)
          ),
          None,
          structAnnotations
        ))
    }

    "handle illegal filenames" in {
      val illegalFilename = "illegal-name.thrift"

      intercept[InvalidThriftFilenameException] {
        getParserForFilenameTest(illegalFilename).parseFile(illegalFilename)
      }
    }

    "handle legal filenames" in {
      val illegalFilename = "legal_name.thrift"
      getParserForFilenameTest(illegalFilename).parseFile(illegalFilename)
    }

    "No thrift keywords as identifiers" in {
      val inputs = Seq(
        "struct MyStruct { 1: optional i64 struct }",
        "struct struct { 1: optional i64 count }",
        "enum list { alpha, beta }",
        "enum Stuff { alpha, beta, include }",
        "exception MyException { 1: string extends }",
        "service service { i32 getNum() }",
        "service MyService { i32 i32() }",
        "service MyService { i32 myMethod(1: bool optional) }",
        "typedef string binary"
      )

      inputs.foreach { code =>
        intercept[KeywordException] {
          parser.parse(code, parser.definition)
        }
      }
    }
  }


  private def getParserForFilenameTest(thriftFilename: String): ThriftParser = {
    val importer = new Importer {
      override def apply(v1: String): scala.Option[FileContents] =
        scala.Some(FileContents(NullImporter, "", scala.Some(thriftFilename)))
      override private[scrooge] def canonicalPaths: Seq[String] = Nil
      override def lastModified(filename: String): scala.Option[Long] = None
      override def getResolvedPath(filename: String): scala.Option[String] = Some(filename)
    }
    new ThriftParser(importer, true)
  }
}
