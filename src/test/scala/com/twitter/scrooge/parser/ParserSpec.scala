package com.twitter.scrooge.parser

import org.specs._
import org.specs.runner._

object ParserSpec extends Specification {
  "The Thrift IDL parser" should {
    val p = new Parser(new Importer())
    import p._

    def parseExpression[T](expr: String, p: Parser[T]) = {
      phrase(p)(new lexical.Scanner(expr)) match {
        case Success(result, _) => result
        case _ => throw new MatchError
      }
    }

    "parse int constants" in {
      parseExpression("1234", intConstant) must_== IntConstant("1234")
      parseExpression("-1234", intConstant) must_== IntConstant("-1234")
      parseExpression("+1234", intConstant) must_== IntConstant("1234")
    }

    "parse double constants" in {
      parseExpression("1.23", doubleConstant) must_== DoubleConstant("1.23")
      parseExpression("-1.23", doubleConstant) must_== DoubleConstant("-1.23")
      parseExpression("1e10", doubleConstant) must_== DoubleConstant("1e10")
    }

    "parse single quoted string literals" in {
      parseExpression("'string lit'", literal) must_== StringLiteral("string lit")
    }

    "parse double quoted string literals" in {
      parseExpression("\"string lit\"", literal) must_== StringLiteral("string lit")
    }

    "parse list constants" in {
      parseExpression("[1, -2.0e3, 3, wee, 'waldo']", constList) must_==
        ConstList(List(IntConstant("1"), DoubleConstant("-2.0e3"), IntConstant("3"),
          Identifier("wee"), StringLiteral("waldo")))
    }

    "parse map constants" in {
      parseExpression("{'jorge': 23, 'tony': 24.5}", constMap) must_==
        ConstMap(Map(StringLiteral("jorge") -> IntConstant("23"),
          StringLiteral("tony") -> DoubleConstant("24.5")))
    }

    "parse all base types" in {
      for ((name, tpe) <- BaseType.map) {
        parseExpression(name, baseType) must_== tpe
      }
    }

    "parse list types" in {
      for ((name, tpe) <- BaseType.map) {
        parseExpression("list<" + name + ">", listType) must_== ListType(tpe, None)
      }
    }

    "parse set types" in {
      for ((name, tpe) <- BaseType.map) {
        parseExpression("set<" + name + ">", setType) must_== SetType(tpe, None)
      }
    }

    "parse map types" in {
      for ((name1, tpe1) <- BaseType.map; (name2, tpe2) <- BaseType.map) {
        parseExpression("map<" + name1 + ", " + name2 + ">", mapType) must_== MapType(tpe1, tpe2, None)
      }
    }

    "parse typedefs" in {
      for ((name, tpe) <- BaseType.map) {
        val ident = "Wee"
        parseExpression("typedef " + name + " " + ident, typedef) must_== Typedef(ident, tpe)
      }
    }

    "parse constant definitions" in {
      parseExpression("const i32 INT32CONSTANT = 9853", const) must_==
        Const("INT32CONSTANT", TI32, IntConstant("9853"))
      parseExpression("const map<string,string> MAPCONSTANT = {'hello':'world', 'goodnight':'moon'}", const) must_==
        Const("MAPCONSTANT", MapType(TString, TString, None),
          ConstMap(Map(StringLiteral("hello") -> StringLiteral("world"),
            StringLiteral("goodnight") -> StringLiteral("moon"))))
    }

    "parse enum definitions" in {
      parseExpression("""
        enum Operation {
          ADD = 1,
          SUBTRACT = 2,
          MULTIPLY = 3,
          DIVIDE = 4
        }
      """, enum) must_== Enum("Operation", List(EnumValue("ADD", 1), EnumValue("SUBTRACT", 2),
                          EnumValue("MULTIPLY", 3), EnumValue("DIVIDE", 4)))
    }

    "parse struct definitions" in {
      parseExpression("""
      struct SharedStruct {
        1: i32 key
        2: string value
      }
      """, struct) must_== Struct("SharedStruct", List(
                            Field(1, "key", TI32, None, false, false),
                            Field(2, "value", TString, None, false, false)))

      parseExpression("""
      struct Work {
        1: i32 num1 = 0,
        2: i32 num2,
        3: Operation op,
        4: optional string comment,
      }
      """, struct) must_== Struct("Work", List(
                            Field(1, "num1", TI32, Some(IntConstant("0")), false, false),
                            Field(2, "num2", TI32, None, false, false),
                            Field(3, "op", ReferenceType("Operation"), None, false, false),
                            Field(4, "comment", TString, None, false, true)))
    }

    "parse exception definitions" in {
      parseExpression("""
        exception InvalidOperation {
          1: i32 what,
          2: string why
        }
      """, exception) must_== Exception("InvalidOperation", List(
                                Field(1, "what", TI32, None, false, false),
                                Field(2, "why", TString, None, false, false)))
    }

    "parse service definitions" in {
      parseExpression("""
      service Calculator extends shared.SharedService {

        /**
         * A method definition looks like C code. It has a return type, arguments,
         * and optionally a list of exceptions that it may throw. Note that argument
         * lists and exception lists are specified using the exact same syntax as
         * field lists in struct or exception definitions.
         */

         void ping(),

         i32 add(1:i32 num1, 2:i32 num2),

         i32 calculate(1:i32 logid, 2:Work w) throws (1:InvalidOperation ouch),

         /**
          * This method has an async modifier. That means the client only makes
          * a request and does not listen for any response at all. Async methods
          * must be void.
          */
         async void zip()

      }
      """, service) must_== Service("Calculator", Some("shared.SharedService"), List(
                              Function("ping", Void, Nil, false, Nil),
                              Function("add", TI32, List(
                                Field(1, "num1", TI32, None, false, false),
                                Field(2, "num2", TI32, None, false, false)), false, Nil),
                              Function("calculate", TI32, List(
                                Field(1, "logid", TI32, None, false, false),
                                Field(2, "w", ReferenceType("Work"), None, false, false)), false, List(
                                  Field(1, "ouch", ReferenceType("InvalidOperation"), None, false, false))),
                              Function("zip", Void, Nil, true, Nil)))
    }

    "parse a whole document" in {
      val doc = parseExpression("""
        # Thrift Tutorial
        # Mark Slee (mcslee@facebook.com)
        #
        # This file aims to teach you how to use Thrift, in a .thrift file. Neato. The
        # first thing to notice is that .thrift files support standard shell comments.
        # This lets you make your thrift file executable and include your Thrift build
        # step on the top line. And you can place comments like this anywhere you like.
        #
        # Before running this file, you will need to have installed the thrift compiler
        # into /usr/local/bin.

        /**
         * The first thing to know about are types. The available types in Thrift are:
         *
         *  bool        Boolean, one byte
         *  byte        Signed byte
         *  i16         Signed 16-bit integer
         *  i32         Signed 32-bit integer
         *  i64         Signed 64-bit integer
         *  double      64-bit floating point value
         *  string      String
         *  binary      Blob (byte array)
         *  map<t1,t2>  Map from one type to another
         *  list<t1>    Ordered list of one type
         *  set<t1>     Set of unique elements of one type
         *
         * Did you also notice that Thrift supports C style comments?
         */

        // Just in case you were wondering... yes. We support simple C comments too.

        /**
         * Thrift files can reference other Thrift files to include common struct
         * and service definitions. These are found using the current path, or by
         * searching relative to any paths specified with the -I compiler flag.
         *
         * Included objects are accessed using the name of the .thrift file as a
         * prefix. i.e. shared.SharedObject
         */
        #include "shared.thrift"

        /**
         * Thrift files can namespace, package, or prefix their output in various
         * target languages.
         */
        namespace cpp tutorial
        namespace java tutorial
        namespace php tutorial
        namespace perl tutorial
        namespace smalltalk.category Thrift.Tutorial

        /**
         * Thrift lets you do typedefs to get pretty names for your types. Standard
         * C style here.
         */
        typedef i32 MyInteger

        /**
         * Thrift also lets you define constants for use across languages. Complex
         * types and structs are specified using JSON notation.
         */
        const i32 INT32CONSTANT = 9853
        const map<string,string> MAPCONSTANT = {'hello':'world', 'goodnight':'moon'}

        /**
         * You can define enums, which are just 32 bit integers. Values are optional
         * and start at 1 if not supplied, C style again.
         */
        enum Operation {
          ADD = 1,
          SUBTRACT = 2,
          MULTIPLY = 3,
          DIVIDE = 4
        }

        /**
         * Structs are the basic complex data structures. They are comprised of fields
         * which each have an integer identifier, a type, a symbolic name, and an
         * optional default value.
         *
         * Fields can be declared "optional", which ensures they will not be included
         * in the serialized output if they aren't set.  Note that this requires some
         * manual management in some languages.
         */
        struct Work {
          1: i32 num1 = 0,
          2: i32 num2,
          3: Operation op,
          4: optional string comment,
        }

        /**
         * Structs can also be exceptions, if they are nasty.
         */
        exception InvalidOperation {
          1: i32 what,
          2: string why
        }

        /**
         * Ahh, now onto the cool part, defining a service. Services just need a name
         * and can optionally inherit from another service using the extends keyword.
         */
        service Calculator extends shared.SharedService {

          /**
           * A method definition looks like C code. It has a return type, arguments,
           * and optionally a list of exceptions that it may throw. Note that argument
           * lists and exception lists are specified using the exact same syntax as
           * field lists in struct or exception definitions.
           */

           void ping(),

           i32 add(1:i32 num1, 2:i32 num2),

           i32 calculate(1:i32 logid, 2:Work w) throws (1:InvalidOperation ouch),

           /**
            * This method has an async modifier. That means the client only makes
            * a request and does not listen for any response at all. Async methods
            * must be void.
            */
           async void zip()

        }

        /**
         * That just about covers the basics. Take a look in the test/ folder for more
         * detailed examples. After you run this file, your generated code shows up
         * in folders with names gen-<language>. The generated code isn't too scary
         * to look at. It even has pretty indentation.
         */
      """, document)

      //println(ScalaGen(doc))

      doc must haveClass[Document]
    }
  }
}
