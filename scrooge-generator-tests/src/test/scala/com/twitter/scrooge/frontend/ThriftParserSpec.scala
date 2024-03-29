package com.twitter.scrooge.frontend

import com.twitter.scrooge.ast.Requiredness.Default
import com.twitter.scrooge.ast._
import com.twitter.scrooge.backend.WithAsClosable
import com.twitter.scrooge.testutil.Spec

class ThriftParserSpec extends Spec {
  "ThriftParser" should {
    val parser = new ThriftParser(NullImporter, true)

    val commentTestSources = Seq(
      "  300  ",
      "  // go away.\n 300",
      "  /*\n   * go away.\n   */\n 300",
      "# hello\n 300",
      "#\n300",
      "# \n300",
      "#    @\n300"
    )

    def verifyCommentParsing(source: String) =
      parser.parse(source, parser.rhs) must be(IntLiteral(300))

    "comments" in {
      commentTestSources.foreach(verifyCommentParsing)
    }

    "comments with Windows-style carriage return" in {
      commentTestSources.map(_.replace("\n", "\r\n")).foreach(verifyCommentParsing)
    }

    "comments with parens" in {
      val source = """
# (
struct MyStruct {}
"""
      parser.parse(source, parser.document) match {
        case Document(List(), List(Struct(SimpleID("MyStruct", None), "MyStruct", List(), None, m)))
            if m.isEmpty =>
        case x => fail(s"Failed to match $x")
      }
    }

    "double-quoted strings" in {
      parser.parse(""" "hello!" """, parser.rhs) must be(StringLiteral("hello!"))
      parser.parse(""" "hello\nthere!" """, parser.rhs) must be(StringLiteral("""hello\nthere!"""))
      parser.parse(""" "hello\\nthere!" """, parser.rhs) must be(
        StringLiteral("""hello\\nthere!""")
      )
      parser.parse(""" "hello//there!" """, parser.rhs) must be(StringLiteral("""hello//there!"""))
      parser.parse(""" "hello'there!" """, parser.rhs) must be(StringLiteral("""hello'there!"""))
      parser.parse(""" "hello\'there!" """, parser.rhs) must be(StringLiteral("""hello\'there!"""))
      parser.parse(""" "hello\"there!" """, parser.rhs) must be(StringLiteral("""hello\"there!"""))
      parser.parse(""" "\"" """, parser.rhs) must be(StringLiteral("\\\""))
    }

    "single-quoted strings" in {
      parser.parse(""" 'hello!' """, parser.rhs) must be(StringLiteral("hello!"))
      parser.parse(""" 'hello\nthere!' """, parser.rhs) must be(StringLiteral("""hello\nthere!"""))
      parser.parse(""" 'hello\\nthere!' """, parser.rhs) must be(
        StringLiteral("""hello\\nthere!""")
      )
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
      parser.parse("{ 'name': 'Commie', 'home': 'San Francisco', }", parser.rhs) must be(
        MapRHS(
          Seq(
            StringLiteral("name") -> StringLiteral("Commie"),
            StringLiteral("home") -> StringLiteral("San Francisco")
          )
        )
      )
    }

    "base types" in {
      parser.parse("i8", parser.fieldType) must be(TByte)
      parser.parse("i16", parser.fieldType) must be(TI16)
      parser.parse("i32", parser.fieldType) must be(TI32)
      parser.parse("i64", parser.fieldType) must be(TI64)
      parser.parse("byte", parser.fieldType) must be(TByte)
      parser.parse("double", parser.fieldType) must be(TDouble)
      parser.parse("string", parser.fieldType) must be(TString)
      parser.parse("bool", parser.fieldType) must be(TBool)
      parser.parse("binary", parser.fieldType) must be(TBinary)
    }

    "list types" in {
      parser.parse("list<i64>", parser.fieldType) must be(ListType(TI64, None))
      parser.parse("list<list<string>>", parser.fieldType) must be(
        ListType(ListType(TString, None), None)
      )
      parser.parse("list < i64 (something = \"else\") >", parser.fieldType) must be(
        ListType(
          AnnotatedFieldType.wrap(
            TI64,
            Map("something" -> "else")
          ),
          None
        )
      )
      parser.parse("list<list<string (a = \"b\")>(c = \"d\")>", parser.fieldType) must be(
        ListType(
          AnnotatedFieldType.wrap(
            ListType(
              AnnotatedFieldType.wrap(
                TString,
                Map("a" -> "b")
              ),
              None
            ),
            Map("c" -> "d")
          ),
          None
        )
      )
    }

    "set types" in {
      parser.parse("set<Monster>", parser.fieldType) must be(
        SetType(ReferenceType(Identifier("Monster")), None)
      )
      parser.parse("set<bool (hello = \"goodbye\")>", parser.fieldType) must be(
        SetType(
          AnnotatedFieldType.wrap(
            TBool,
            Map("hello" -> "goodbye")
          ),
          None
        )
      )
    }

    "map types" in {
      parser.parse("map<string, list<bool>>", parser.fieldType) must be(
        MapType(TString, ListType(TBool, None), None)
      )
      parser.parse(
        "map<string (a=\"b\"), list<bool (c=\"d\")> (e=\"f\")>",
        parser.fieldType) must be(
        MapType(
          AnnotatedFieldType.wrap(
            TString,
            Map("a" -> "b")
          ),
          AnnotatedFieldType.wrap(
            ListType(
              AnnotatedFieldType.wrap(
                TBool,
                Map("c" -> "d")
              ),
              None
            ),
            Map("e" -> "f")
          ),
          None
        )
      )
    }

    "inner collection types annotations" in {
      parser.parse(
        """list<map<set<i32 (python.immutable = "0")> (python.immutable = "1"), map<i32,set<list<map<Insanity,string>(python.immutable = "2")> (python.immutable = "3")>>>>""",
        parser.fieldType
      ) must be(
        ListType(
          MapType(
            AnnotatedFieldType.wrap(
              SetType(
                AnnotatedFieldType.wrap(
                  TI32,
                  Map("python.immutable" -> "0")
                ),
                None
              ),
              Map("python.immutable" -> "1")
            ),
            MapType(
              TI32,
              SetType(
                AnnotatedFieldType.wrap(
                  ListType(
                    AnnotatedFieldType.wrap(
                      MapType(
                        ReferenceType(
                          Identifier("Insanity")
                        ),
                        TString,
                        None
                      ),
                      Map("python.immutable" -> "2")
                    ),
                    None
                  ),
                  Map("python.immutable" -> "3")
                ),
                None
              ),
              None
            ),
            None
          ),
          None
        )
      )
    }

    "compound types" in {
      parser.parse("Monster", parser.fieldType) must be(ReferenceType(Identifier("Monster")))
    }

    "functions" in {
      parser.parse("/**doc!*/ void go()", parser.function) must be(
        Function(SimpleID("go"), "go", Void, Seq(), Seq(), Some("/**doc!*/"))
      )
      parser.parse(
        "list<string> get_tables(optional i32 id, /**DOC*/3: required string name='cat') throws (1: Exception ex);",
        parser.function
      ) must be(
        Function(
          SimpleID("get_tables"),
          "get_tables",
          ListType(TString, None),
          Seq(
            Field(-1, SimpleID("id"), "id", TI32, None, Requiredness.Optional),
            Field(
              3,
              SimpleID("name"),
              "name",
              TString,
              Some(StringLiteral("cat")),
              Requiredness.Required,
              docstring = Some("/**DOC*/")
            )
          ),
          Seq(
            Field(
              1,
              SimpleID("ex"),
              "ex",
              ReferenceType(Identifier("Exception")),
              None,
              Requiredness.Default
            )
          ),
          None
        )
      )
    }

    "const" in {
      parser.parse("/** COMMENT */ const string name = \"Columbo\"", parser.definition) must be(
        ConstDefinition(SimpleID("name"), TString, StringLiteral("Columbo"), Some("/** COMMENT */"))
      )
    }

    "more than one docstring" in {
      val code = """
/** comment */
/** and another */
const string tyrion = "lannister"
"""
      parser.parse(code, parser.definition) must be(
        ConstDefinition(
          SimpleID("tyrion"),
          TString,
          StringLiteral("lannister"),
          Some("/** comment */\n/** and another */")
        )
      )
    }

    "comment before docstring" in {
      val code = """
#
/** docstring */
const string tyrion = "lannister"
"""
      parser.parse(code, parser.definition) must be(
        ConstDefinition(
          SimpleID("tyrion"),
          TString,
          StringLiteral("lannister"),
          Some("/** docstring */")
        )
      )
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
        )
      )
    }

    "enum" in {
      val code = """
        enum Direction {
          NORTH, SOUTH, EAST=90, WEST, UP, DOWN=5
        }
        """
      parser.parse(code, parser.definition) must be(
        Enum(
          SimpleID("Direction"),
          Seq(
            EnumField(SimpleID("NORTH"), 0, None),
            EnumField(SimpleID("SOUTH"), 1, None),
            EnumField(SimpleID("EAST"), 90, None),
            EnumField(SimpleID("WEST"), 91, None),
            EnumField(SimpleID("UP"), 92, None),
            EnumField(SimpleID("DOWN"), 5, None)
          ),
          None
        )
      )

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
      parser.parse(withComment, parser.enum) must be(
        Enum(
          SimpleID("Foo"),
          Seq(
            EnumField(SimpleID("X"), 1, Some("/** I am a doc. */")),
            EnumField(SimpleID("Y"), 2, None)
          ),
          Some("/**\n * Docstring!\n */")
        )
      )
    }

    "senum" in {
      // wtf is senum?!
      parser.parse("senum Cities { 'Milpitas', 'Mayfield' }", parser.definition) must be(
        Senum(SimpleID("Cities"), Seq("Milpitas", "Mayfield"))
      )
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
      parser.parse(code, parser.definition) must be(
        Struct(
          SimpleID("Point"),
          "Point",
          Seq(
            Field(1, SimpleID("x"), "x", TDouble, None, Requiredness.Default),
            Field(
              2,
              SimpleID("y"),
              "y",
              TDouble,
              None,
              Requiredness.Default,
              docstring = Some("/** comments*/")
            ),
            Field(
              3,
              SimpleID("color"),
              "color",
              ReferenceType(Identifier("Color")),
              Some(IdRHS(SimpleID("BLUE"))),
              Requiredness.Default
            )
          ),
          Some("/** docs up here */"),
          Map("annotation" -> "supported", "multiline" -> "also supported")
        )
      )
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
        parser.parse(code, parser.definition) must be(
          Union(
            SimpleID("Aircraft"),
            "Aircraft",
            Seq(
              Field(
                1,
                SimpleID("a"),
                "a",
                ReferenceType(Identifier("Airplane")),
                None,
                Requiredness.Default
              ),
              Field(
                2,
                SimpleID("r"),
                "r",
                ReferenceType(Identifier("Rotorcraft")),
                None,
                Requiredness.Default,
                docstring = Some("/** comments*/")
              ),
              Field(
                3,
                SimpleID("g"),
                "g",
                ReferenceType(Identifier("Glider")),
                None,
                Requiredness.Default
              ),
              Field(
                4,
                SimpleID("lta"),
                "lta",
                ReferenceType(Identifier("LighterThanAir")),
                None,
                Requiredness.Default
              )
            ),
            Some("/** docs up here */"),
            Map("maxTypes" -> "4")
          )
        )
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

        laxParser.parse(code, laxParser.definition) must be(
          Union(
            SimpleID("Aircraft"),
            "Aircraft",
            Seq(
              Field(
                1,
                SimpleID("a"),
                "a",
                ReferenceType(Identifier("Airplane")),
                None,
                Requiredness.Default
              ),
              Field(
                2,
                SimpleID("r"),
                "r",
                ReferenceType(Identifier("Rotorcraft")),
                None,
                Requiredness.Default
              ),
              Field(
                3,
                SimpleID("g"),
                "g",
                ReferenceType(Identifier("Glider")),
                None,
                Requiredness.Default
              )
            ),
            None,
            Map.empty
          )
        )
      }

      "invalid field name" in {
        intercept[UnionFieldInvalidNameException] {
          parser.parse(
            """
            union Fruit {
              1: Apple apple
              2: Banana banana
              3: UnknownFruit unknown_union_field
            }
          """,
            parser.definition
          )
        }
      }
    }

    "exception" in {
      parser.parse("exception BadError { 1: string message }", parser.definition) must be(
        Exception_(
          SimpleID("BadError"),
          "BadError",
          Seq(Field(1, SimpleID("message"), "message", TString, None, Requiredness.Default)),
          None
        )
      )
      parser.parse("exception E { string message, string reason }", parser.definition) must be(
        Exception_(
          SimpleID("E"),
          "E",
          Seq(
            Field(-1, SimpleID("message"), "message", TString, None, Requiredness.Default),
            Field(-2, SimpleID("reason"), "reason", TString, None, Requiredness.Default)
          ),
          None
        )
      )
      parser.parse("exception NoParams { }", parser.definition) must be(
        Exception_(SimpleID("NoParams"), "NoParams", Seq(), None)
      )
      parser.parse(
        "/** doc rivers */ exception wellDocumentedException { }",
        parser.definition) must be(
        Exception_(
          SimpleID("wellDocumentedException"),
          "wellDocumentedException",
          Seq(),
          Some("/** doc rivers */")
        )
      )

      val annotations = Map("persisted" -> "true")
      parser.parse(
        "exception BadError { 1: string message } (persisted = \"true\")",
        parser.definition
      ) must be(
        Exception_(
          SimpleID("BadError"),
          "BadError",
          Seq(Field(1, SimpleID("message"), "message", TString, None, Requiredness.Default)),
          None,
          annotations
        )
      )
    }

    "service" in {
      val code = """
        /** cold hard cache */
        service Cache {
          void put(1: string name, 2: binary value) (mode="LRU");
          binary get(1: string name) throws (1: NotFoundException ex);
        }
                 """
      parser.parse(code, parser.definition) must be(
        Service(
          SimpleID("Cache"),
          None,
          Seq(
            Function(
              SimpleID("put"),
              "put",
              Void,
              Seq(
                Field(1, SimpleID("name"), "name", TString, None, Requiredness.Default),
                Field(2, SimpleID("value"), "value", TBinary, None, Requiredness.Default)
              ),
              Seq(),
              None,
              Map("mode" -> "LRU")
            ),
            Function(
              SimpleID("get"),
              "get",
              TBinary,
              Seq(
                Field(1, SimpleID("name"), "name", TString, None, Requiredness.Default)
              ),
              Seq(
                Field(
                  1,
                  SimpleID("ex"),
                  "ex",
                  ReferenceType(Identifier("NotFoundException")),
                  None,
                  Requiredness.Default
                )
              ),
              None
            )
          ),
          Some("/** cold hard cache */"),
          Map(),
          Set(WithAsClosable)
        )
      )

      parser.parse("service LeechCache extends Cache {}", parser.definition) must be(
        Service(
          SimpleID("LeechCache"),
          Some(ServiceParent(SimpleID("Cache"), None)),
          Seq(),
          None,
          Map(),
          Set(WithAsClosable)
        )
      )
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
      parser.parse(code, parser.document) must be(
        Document(
          Seq(Namespace("java", Identifier("com.example")), Namespace("*", Identifier("example"))),
          Seq(
            Service(
              SimpleID("NullService"),
              None,
              Seq(
                Function(SimpleID("doNothing"), "doNothing", Void, Seq(), Seq(), Some("/** DoC */"))
              ),
              Some("/** what up doc */"),
              Map(),
              Set(WithAsClosable)
            )
          )
        )
      )
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
        )
      )

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
            Field(
              1,
              SimpleID("id"),
              "id",
              TI64,
              Some(IntLiteral(0)),
              Requiredness.Default,
              idTypeAnnotations,
              idFieldAnnotations
            ),
            Field(
              2,
              SimpleID("code"),
              "code",
              TString,
              None,
              Requiredness.Optional,
              codeTypeAnnotations,
              Map.empty
            ),
            Field(
              3,
              SimpleID("name"),
              "name",
              TString,
              None,
              Requiredness.Optional,
              Map.empty,
              nameFieldAnnotations
            )
          ),
          None,
          structAnnotations
        )
      )
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

    "boolean default values" in {
      var field = parser.parse("bool x = 0", parser.field)
      field.default must be(Some(BoolLiteral(false)))

      field = parser.parse("bool x = 1", parser.field)
      field.default must be(Some(BoolLiteral(true)))

      intercept[TypeMismatchException] {
        parser.parse("bool x = 2", parser.field)
      }

      field = parser.parse("bool x = false", parser.field)
      field.default must be(Some(BoolLiteral(false)))

      field = parser.parse("bool x = true", parser.field)
      field.default must be(Some(BoolLiteral(true)))

      field = parser.parse("bool x = False", parser.field)
      field.default must be(Some(BoolLiteral(false)))

      field = parser.parse("bool x = True", parser.field)
      field.default must be(Some(BoolLiteral(true)))

      intercept[TypeMismatchException] {
        parser.parse("bool x = WhatIsThis", parser.field)
      }

      parser.parse("const bool z = false", parser.const) must be(
        ConstDefinition(SimpleID("z", None), TBool, BoolLiteral(false), None)
      )

      parser.parse("const bool z = True", parser.const) must be(
        ConstDefinition(SimpleID("z", None), TBool, BoolLiteral(true), None)
      )

      intercept[TypeMismatchException] {
        parser.parse("const bool z = IDontEven", parser.const)
      }

      intercept[TypeMismatchException] {
        parser.parse(
          "service theService { i32 getValue(1: bool arg = SomethingElse) }",
          parser.service
        )
      }

      parser.parse("struct asdf { bool x = false }", parser.struct) must be(
        Struct(
          SimpleID("asdf", None),
          "asdf",
          List(
            Field(
              -1,
              SimpleID("x", None),
              "x",
              TBool,
              Some(BoolLiteral(false)),
              Requiredness.Default,
              Map(),
              Map()
            )
          ),
          None,
          Map()
        )
      )

      parser.parse("struct asdf { bool x = 1 }", parser.struct) must be(
        Struct(
          SimpleID("asdf", None),
          "asdf",
          List(
            Field(
              -1,
              SimpleID("x", None),
              "x",
              TBool,
              Some(BoolLiteral(true)),
              Requiredness.Default,
              Map(),
              Map()
            )
          ),
          None,
          Map()
        )
      )

      intercept[TypeMismatchException] {
        parser.parse("struct S { 1: bool B = 15 }", parser.struct)
      }
    }

    "Apache-compatible annotations" in {
      // see https://svn.apache.org/viewvc/thrift/trunk/test/AnnotationTest.thrift?view=markup&pathrev=1386848

      parser.parse(
        """typedef list<i32> ( cpp.template = "std::list" ) int_linked_list""",
        parser.typedef
      ) must be(
        Typedef(
          SimpleID("int_linked_list", None),
          ListType(TI32, None),
          Map("cpp.template" -> "std::list")
        )
      )

      parser.parse(
        """typedef string ( unicode.encoding = "UTF-16" ) non_latin_string (foo="bar")""",
        parser.typedef
      ) must be(
        Typedef(
          SimpleID("non_latin_string", None),
          TString,
          Map("unicode.encoding" -> "UTF-16"),
          Map("foo" -> "bar")
        )
      )

      parser.parse(
        """typedef list< double ( cpp.fixed_point = "16" ) > tiny_float_list""",
        parser.typedef
      ) must be(
        Typedef(
          SimpleID("tiny_float_list", None),
          ListType(
            AnnotatedFieldType.wrap(
              TDouble,
              Map("cpp.fixed_point" -> "16")
            ),
            None
          ),
          Map(),
          Map()
        )
      )

      parser.parse(
        """
          |struct foo {
          |  1: i32 bar ( presence = "required" );
          |  2: i32 baz ( presence = "manual", cpp.use_pointer = "", );
          |  3: i32 qux;
          |  4: i32 bop;
          |} (
          |  cpp.type = "DenseFoo",
          |  python.type = "DenseFoo",
          |  java.final = "",
          |)
        """.stripMargin,
        parser.struct
      ) must be(
        Struct(
          SimpleID("foo", None),
          "foo",
          Seq(
            Field(
              1,
              SimpleID("bar", None),
              "bar",
              TI32,
              None,
              Default,
              Map(),
              Map("presence" -> "required"),
              None
            ),
            Field(
              2,
              SimpleID("baz", None),
              "baz",
              TI32,
              None,
              Default,
              Map(),
              Map("presence" -> "manual", "cpp.use_pointer" -> ""),
              None
            ),
            Field(3, SimpleID("qux", None), "qux", TI32, None, Default, Map(), Map(), None),
            Field(4, SimpleID("bop", None), "bop", TI32, None, Default, Map(), Map(), None)
          ),
          None,
          Map("cpp.type" -> "DenseFoo", "python.type" -> "DenseFoo", "java.final" -> "")
        )
      )

      parser.parse(
        """
        |exception foo_error {
        |  1: i32 error_code ( foo="bar" )
        |  2: string error_msg
        |} (foo = "bar")
        |
      """.stripMargin,
        parser.exception
      ) must be(
        Exception_(
          SimpleID("foo_error", None),
          "foo_error",
          Seq(
            Field(
              1,
              SimpleID("error_code", None),
              "error_code",
              TI32,
              None,
              Default,
              Map(),
              Map("foo" -> "bar"),
              None
            ),
            Field(
              2,
              SimpleID("error_msg", None),
              "error_msg",
              TString,
              None,
              Default,
              Map(),
              Map(),
              None
            )
          ),
          None,
          Map("foo" -> "bar")
        )
      )

      parser.parse(
        """
        |enum weekdays {
        |  SUNDAY ( weekend = "yes" ),
        |  MONDAY,
        |  TUESDAY,
        |  WEDNESDAY,
        |  THURSDAY,
        |  FRIDAY,
        |  SATURDAY ( weekend = "yes" )
        |} (foo.bar="baz")
        |
      """.stripMargin,
        parser.enum
      ) must be(
        Enum(
          SimpleID("weekdays", None),
          Seq(
            EnumField(SimpleID("SUNDAY", None), 0, None, Map("weekend" -> "yes")),
            EnumField(SimpleID("MONDAY", None), 1, None, Map()),
            EnumField(SimpleID("TUESDAY", None), 2, None, Map()),
            EnumField(SimpleID("WEDNESDAY", None), 3, None, Map()),
            EnumField(SimpleID("THURSDAY", None), 4, None, Map()),
            EnumField(SimpleID("FRIDAY", None), 5, None, Map()),
            EnumField(SimpleID("SATURDAY", None), 6, None, Map("weekend" -> "yes"))
          ),
          None,
          Map("foo.bar" -> "baz")
        )
      )

      // Annotations on senum values are not supported
      parser.parse(
        """
        |senum seasons {
        |  "Spring",
        |  "Summer",
        |  "Fall",
        |  "Winter"
        |} ( foo = "bar" )
        |
      """.stripMargin,
        parser.senum
      ) must be(
        Senum(
          SimpleID("seasons", None),
          Seq("Spring", "Summer", "Fall", "Winter"),
          Map("foo" -> "bar")
        )
      )

      parser.parse(
        """
        |service foo_service {
        |  void foo() ( foo = "bar" )
        |} (a.b="c")
        |
      """.stripMargin,
        parser.service) must be(
        Service(
          SimpleID("foo_service", None),
          None,
          Seq(
            Function(SimpleID("foo", None), "foo", Void, Seq(), Seq(), None, Map("foo" -> "bar"))
          ),
          None,
          Map("a.b" -> "c"),
          Set(WithAsClosable)
        )
      )
    }
  }

  private def getParserForFilenameTest(thriftFilename: String): ThriftParser = {
    val importer = new Importer {
      override def apply(v1: String): scala.Option[FileContents] =
        scala.Some(FileContents(NullImporter, "", scala.Some(thriftFilename)))
      override private[scrooge] def canonicalPaths: Seq[String] = Nil
      override def lastModified(filename: String): scala.Option[Long] = None
      override private[scrooge] def getResolvedPath(filename: String): scala.Option[String] =
        Some(filename)
    }
    new ThriftParser(importer, true)
  }
}
