package com.twitter.scrooge

import org.specs.SpecificationWithJUnit

class TypeResolverSpec extends SpecificationWithJUnit {
  import AST._

  "TypeResolve" should {
    val foo = EnumValue("FOO", 1)
    val bar = EnumValue("BAR", 2)
    val enum = Enum("SomeEnum", Seq(foo, bar), None)
    val enumType = EnumType(enum)
    val enumRef = ReferenceType(enum.name)
    val struct = Struct("BlahBlah", Seq(
      Field(1, "baby", TI16),
      Field(2, "mama", TI32),
      Field(3, "papa", TI64),
      Field(4, "pupu", enumRef)
    ), None)
    val structType = StructType(struct)
    val structRef = ReferenceType(struct.name)
    val ex = Exception_("Boom", Seq(Field(1, "msg", enumRef)), None)
    val exType = StructType(ex)
    val exRef = ReferenceType(ex.name)
    val resolver = TypeResolver()
      .define(enum.name, enumType)
      .define(struct.name, structType)
      .define(ex.name, exType)

    "throw exception on unknown type" in {
      resolver(ReferenceType("wtf")) must throwA[TypeNotFoundException]
    }

    "resolve a known type" in {
      resolver(enumRef) mustEqual enumType
    }

    "resolve dependent types" in {
      TypeResolver().resolve(enum, None) must beLike {
        case ResolvedDefinition(enum2, resolver2) =>
          resolver2.resolve(struct, None) must beLike {
            case ResolvedDefinition(struct2: Struct, _) =>
              struct2.fields(3).`type` mustEqual enumType
              true
          }
          true
      }
    }

    "resolve a scoped type" in {

    }

    "transform MapType" in {
      resolver(MapType(enumRef, structRef, None)) must beLike {
        case MapType(`enumType`, `structType`, None) => true
      }
    }

    "transform SetType" in {
      resolver(SetType(structRef, None)) must beLike {
        case SetType(`structType`, None) => true
      }
    }

    "transform ListType" in {
      resolver(ListType(structRef, None)) must beLike {
        case ListType(`structType`, None) => true
      }
    }

    "not break on Void" in {
      resolver(Void) mustEqual Void
    }

    "transform a Field" in {
      val field = Field(42, "foo", structRef)
      resolver(field) mustEqual field.copy(`type` = structType)
    }

    "transform a Field with enum constant default" in {
      val field = Field(1, "foo", enumRef, Some(Identifier("FOO")))
      resolver(field) mustEqual
        Field(1, "foo", enumType, Some(EnumValueConstant(enum, foo)))
    }

    "transform a Function" in {
      val field = Field(1, "foo", structRef)
      val ex = Field(2, "ex", structRef)
      val fun = Function("foo", structRef, Seq(field), Seq(ex), None)
      resolver(fun) mustEqual
        Function("foo", resolver(fun.`type`), Seq(resolver(field)), Seq(resolver(ex)), None)
    }

    "transform a TypeDef" in {
      val typedef = Typedef("foo", enumRef)
      resolver(typedef) mustEqual typedef.copy(`type` = enumType)
    }

    "transform a Struct" in {
      resolver(struct) mustEqual struct.copy(fields = struct.fields.map(resolver.apply))
    }

    "transform an Exception" in {
      resolver(ex) mustEqual ex.copy(fields = ex.fields.map(resolver.apply))
    }

    "transform a Const" in {
      val const = Const("foo", enumRef, Identifier("FOO"), None)
      resolver(const) mustEqual Const("foo", enumType, EnumValueConstant(enum, enum.values.head), None)
    }

    "transform a Service" in {
      val fun = Function("foo", structRef, Seq(Field(1, "foo", structRef)), Nil, None)
      val service = Service("Glurb", None, Seq(fun), None)
      resolver(service) mustEqual service.copy(functions = Seq(resolver(fun)))
    }

    "resolve a service parent from same scope" in {
      val service1 = Service("Super", None, Nil, None)
      val service2 = Service("Sub", Some(ServiceParent("Super")), Nil, None)
      val resolver = TypeResolver().define(service1)
      resolver(service2) mustEqual service2.copy(parent = Some(ServiceParent(service1)))
    }

    "resolve a service parent from an included scope" in {
      val service1 = Service("Super", None, Nil, None)
      val otherDoc = Document(Nil, Seq(service1))
      val include = Include("other.thrift", otherDoc)
      val service2 = Service("Sub", Some(ServiceParent("other.Super")), Nil, None)
      val resolver = TypeResolver().include(include)
      resolver(service2) mustEqual service2.copy(parent = Some(ServiceParent("other.Super", Some(service1))))
    }

    "resolve a typedef from an included scope" in {
      val oneInt = Struct("OneInt", Seq(Field(1, "id", TI32, None, Requiredness.Default)), None)
      val typedefInt = Typedef("ManyInts", ListType(ReferenceType("OneInt"), None))
      val doc1 = Document(Nil, Seq(oneInt, typedefInt))

      val collectionStruct = Struct("IntCollection", Seq(
        Field(1, "scores1", ReferenceType("typedef1.ManyInts"), None, Requiredness.Default),
        Field(2, "scores2", SetType(ReferenceType("typedef1.OneInt"), None), None, Requiredness.Default)
      ), None)
      val doc2 = Document(Seq(Include("typedef1.thrift", doc1)), Seq(collectionStruct))

      val resolvedDoc = TypeResolver().resolve(doc2).document
      resolvedDoc.defs(0) must beLike {
        case Struct(name, fields, _) => {
          fields(0) must beLike {
            case Field(1, _, ListType(StructType(_, Some("typedef1")), None), _, _) => true
          }
          fields(1) must beLike {
            case Field(2, _, SetType(StructType(_, Some("typedef1")), None), _, _) => true
          }
        }
      }
    }
  }
}
