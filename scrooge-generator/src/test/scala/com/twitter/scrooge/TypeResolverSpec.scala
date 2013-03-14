package com.twitter.scrooge

import org.specs.SpecificationWithJUnit
import com.twitter.scrooge.ast._
class TypeResolverSpec extends SpecificationWithJUnit {
  "TypeResolve" should {
    val foo = EnumField(SimpleID("FOO"), 1, None)
    val bar = EnumField(SimpleID("BAR"), 2, Some("/** I am a doc. */"))
    val enum = Enum(SimpleID("SomeEnum"), Seq(foo, bar), None)
    val enumType = new EnumType(enum)
    val enumRef = ReferenceType(enum.sid)
    val struct = Struct(SimpleID("BlahBlah"), "BlahBlah", Seq(
      Field(1, SimpleID("baby"), "baby", TI16),
      Field(2, SimpleID("mama"), "mama", TI32),
      Field(3, SimpleID("papa"), "papa", TI64),
      Field(4, SimpleID("pupu"), "pupu", enumRef)
    ), None)
    val structType = new StructType(struct)
    val structRef = ReferenceType(struct.sid)
    val ex = Exception_(SimpleID("Boom"), "Boom", Seq(Field(1, SimpleID("msg"), "msg", enumRef)), None)
    val exType = new StructType(ex)
    val exRef = ReferenceType(ex.sid)
    val resolver = TypeResolver()
      .withMapping(enum.sid.name, enumType)
      .withMapping(struct.sid.name, structType)
      .withMapping(ex.sid.name, exType)

    "throw exception on unknown type" in {
      resolver(ReferenceType(Identifier("wtf"))) must throwA[TypeNotFoundException]
    }

    "resolve a known type" in {
      resolver(enumRef) mustEqual enumType
    }

    "resolve dependent types" in {
      TypeResolver()(enum, None) must beLike {
        case ResolvedDefinition(enum2, resolver2) =>
          resolver2(struct, None) must beLike {
            case ResolvedDefinition(struct2: Struct, _) =>
              struct2.fields(3).fieldType mustEqual enumType
              true
          }
          true
      }
    }

    "resolve a scoped type" in {
      //todo: CSL-272
    }

    "transform MapType" in {
      resolver(MapType(enumRef, structRef, None)) must beLike {
        case MapType(enumType, structType, None) => true
      }
    }

    "transform SetType" in {
      resolver(SetType(structRef, None)) must beLike {
        case SetType(structType, None) => true
      }
    }

    "transform ListType" in {
      resolver(ListType(structRef, None)) must beLike {
        case ListType(structType, None) => true
      }
    }

    "not break on Void" in {
      resolver(Void) mustEqual Void
    }

    "transform a Field" in {
      val field = Field(42, SimpleID("foo"), "foo", structRef)
      resolver(field) mustEqual field.copy(fieldType = structType)
    }

    "transform a Field with enum constant default" in {
      val field = Field(1, SimpleID("field"), "field", enumRef, Some(IdRHS(Identifier("SomeEnum.FOO"))))
      resolver(field) mustEqual
        Field(1, SimpleID("field"), "field", enumType, Some(EnumRHS(enum, foo)))
    }

    "transform a Function" in {
      val field = Field(1, SimpleID("foo"), "foo", structRef)
      val ex = Field(2, SimpleID("ex"), "ex", structRef)
      val fun = Function(SimpleID("foo"), "foo", structRef, Seq(field), Seq(ex), None)
      resolver(fun) mustEqual
        Function(SimpleID("foo"), "foo", resolver(fun.funcType), Seq(resolver(field)), Seq(resolver(ex)), None)
    }

    "transform a TypeDef" in {
      val typedef = Typedef(SimpleID("foo"), enumRef)
      resolver(typedef, None).definition mustEqual typedef.copy(fieldType = enumType)
    }

    "transform a Struct" in {
      resolver(struct, None).definition mustEqual struct.copy(fields = struct.fields.map(resolver.apply))
    }

    "transform an Exception" in {
      resolver(ex, None).definition mustEqual ex.copy(fields = ex.fields.map(resolver.apply))
    }

    "transform a Const" in {
      val const = ConstDefinition(SimpleID("foo"), enumRef, IdRHS(Identifier("SomeEnum.FOO")), None)
      resolver(const, None).definition mustEqual ConstDefinition(SimpleID("foo"), enumType, EnumRHS(enum, foo), None)
    }

    "const definition transitivity" in {
      // this code is ok
      //   const string line = "hi"
      //   const string copy = line
      val line = ConstDefinition(SimpleID("line"), TString, StringLiteral("hi"), None)
      val newResolver = resolver(line, None).resolver
      val copy = ConstDefinition(SimpleID("copy"), TString, IdRHS(SimpleID("line")), None)
      newResolver(copy, None).definition mustEqual
        ConstDefinition(SimpleID("copy"), TString, StringLiteral("hi"), None)

      // this code has type mismatch
      //   const string line = "hi"
      //   const i32 copy = line
      //
      val copyWrongType = ConstDefinition(SimpleID("copy"), TI32, IdRHS(SimpleID("line")), None)
      newResolver(copyWrongType, None) must throwA[TypeMismatchException]

      // this code has undefined symbol
      //   const string line = "hi"
      //   const string copy = noSuchConst
      val copyWrongId = ConstDefinition(SimpleID("copy"), TString, IdRHS(SimpleID("noSuchConst")), None)
      newResolver(copyWrongId, None) must throwA[UndefinedConstantException]
    }

    "transform a Service" in {
      val fun = Function(SimpleID("foo"), "foo", structRef,
        Seq(Field(1, SimpleID("foo"), "foo", structRef)), Nil, None)
      val service = Service(SimpleID("Glurb"), None, Seq(fun), None)
      resolver(service, None).definition mustEqual service.copy(functions = Seq(resolver(fun)))
    }

    "resolve a service parent from same scope" in {
      val service1 = Service(SimpleID("Super"), None, Nil, None)
      val service2 = Service(
        SimpleID("Sub"),
        Some(ServiceParent(SimpleID("Super"), None)),
        Nil,
        None)
      val resolver = TypeResolver().withMapping(service1)
      resolver(service2, None).definition mustEqual service2.copy(parent =
        Some(ServiceParent(
          service1.sid,
          None,
          Some(service1))))
    }

    "resolve a service parent from an included scope" in {
      val service1 = Service(SimpleID("Super"), None, Nil, None)
      val otherDoc = Document(Nil, Seq(service1))
      val include = Include("other.thrift", otherDoc)
      val service2 = Service(
        SimpleID("Sub"),
        Some(ServiceParent(SimpleID("Super"), Some(SimpleID("other")))),
        Nil,
        None)
      val resolver = TypeResolver().withMapping(include)
      resolver(service2, None).definition mustEqual
        service2.copy(parent = Some(ServiceParent(
          SimpleID("Super"),
          Some(SimpleID("other")),
          Some(service1))))
    }

    "resolve a typedef from an included scope" in {
      val oneInt = Struct(SimpleID("OneInt"), "OneInt",
        Seq(Field(1, SimpleID("id"), "id", TI32, None, Requiredness.Default)), None)
      val typedefInt = Typedef(SimpleID("ManyInts"), ListType(ReferenceType(Identifier("OneInt")), None))
      val doc1 = Document(Nil, Seq(oneInt, typedefInt))

      val collectionStruct = Struct(SimpleID("IntCollection"), "IntCollection", Seq(
        Field(1, SimpleID("scores1"), "scores1", ReferenceType(Identifier("typedef1.ManyInts")), None, Requiredness.Default),
        Field(2, SimpleID("scores2"), "scores2", SetType(ReferenceType(Identifier("typedef1.OneInt")), None), None, Requiredness.Default)
      ), None)
      val doc2 = Document(Seq(Include("src/test/thrift/typedef1.thrift", doc1)), Seq(collectionStruct))

      val resolvedDoc = TypeResolver()(doc2).document
      resolvedDoc.defs(0) must beLike {
        case Struct(_, _, fields, _) => {
          fields(0) must beLike {
            case Field(1, _, _, ListType(StructType(_, Some(SimpleID("typedef1"))), None), _, _) => true
          }
          fields(1) must beLike {
            case Field(2, _, _, SetType(StructType(_, Some(SimpleID("typedef1"))), None), _, _) => true
          }
        }
      }
    }
  }
}
