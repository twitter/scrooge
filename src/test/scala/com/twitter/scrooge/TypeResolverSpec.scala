package com.twitter.scrooge

import org.specs.Specification

object TypeResolverSpec extends Specification {
  import AST._

  "TypeResolve" should {
    val foo = EnumValue("FOO", 1)
    val bar = EnumValue("BAR", 2)
    val enum = Enum("SomeEnum", Seq(foo, bar))
    val enumType = EnumType(enum)
    val enumRef = ReferenceType(enum.name)
    val struct = Struct("BlahBlah", Seq(
      Field(1, "baby", TI16),
      Field(2, "mama", TI32),
      Field(3, "papa", TI64),
      Field(4, "pupu", enumRef)
    ))
    val structType = StructType(struct)
    val structRef = ReferenceType(struct.name)
    val ex = Exception_("Boom", Seq(Field(1, "msg", enumRef)))
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
      TypeResolver().resolve(enum) must beLike {
        case ResolvedDefinition(enum2, resolver2) =>
          resolver2.resolve(struct) must beLike {
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
      val fun = Function("foo", structRef, Seq(field), false, Seq(ex))
      resolver(fun) mustEqual
        Function("foo", resolver(fun.`type`), Seq(resolver(field)), false, Seq(resolver(ex)))
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
      val const = Const("foo", enumRef, Identifier("FOO"))
      resolver(const) mustEqual Const("foo", enumType, EnumValueConstant(enum, enum.values.head))
    }

    "transform a Service" in {
      val fun = Function("foo", structRef, Seq(Field(1, "foo", structRef)), false, Nil)
      val service = Service("Glurb", None, Seq(fun))
      resolver(service) mustEqual service.copy(functions = Seq(resolver(fun)))
    }
  }
}
