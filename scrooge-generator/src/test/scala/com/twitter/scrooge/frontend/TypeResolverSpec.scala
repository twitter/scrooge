package com.twitter.scrooge.frontend

import com.twitter.scrooge.ast._
import com.twitter.scrooge.testutil.Spec

class TypeResolverSpec extends Spec {
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
    ), None, Map.empty)
    val structType = new StructType(struct)
    val structRef = ReferenceType(struct.sid)
    val ex = Exception_(SimpleID("Boom"), "Boom", Seq(Field(1, SimpleID("msg"), "msg", enumRef)), None)
    val exType = new StructType(ex)
    val resolver = TypeResolver()
      .withMapping(enum.sid.name, enumType)
      .withMapping(struct.sid.name, structType)
      .withMapping(ex.sid.name, exType)

    def createStruct(structName: String, fieldType: FieldType) = {
      val fieldName: String = structName + "_field"
      Struct(SimpleID(structName), structName, Seq(Field(1, SimpleID(fieldName), fieldName, fieldType)), None, Map.empty)
    }

    "throw exception on unknown type" in {
      intercept[TypeNotFoundException] {
        resolver(ReferenceType(Identifier("wtf")))
      }
    }

    "resolve a known type" in {
      resolver(enumRef) must be(enumType)
    }

    "resolve dependent types" in {
      TypeResolver()(enum, None) match {
        case ResolvedDefinition(enum2, resolver2) =>
          resolver2(struct, None) match {
            case ResolvedDefinition(struct2: Struct, _) =>
              struct2.fields(3).fieldType must be(enumType)
              // pass
            case _ =>
              fail()
          }
        case _ => fail()
      }
    }

    "transform MapType" in {
      resolver(MapType(enumRef, structRef, None)) match {
        case MapType(enumType, structType, None) =>
          // pass
        case _ =>
          fail()
      }
    }

    "transform SetType" in {
      resolver(SetType(structRef, None)) match {
        case SetType(structType, None) =>
          // pass
        case _ =>
          fail()
      }
    }

    "transform ListType" in {
      resolver(ListType(structRef, None)) match {
        case ListType(structType, None) =>
          // pass
        case _ =>
          fail()
      }
    }

    "not break on Void" in {
      resolver(Void) must be(Void)
    }

    "transform a Field" in {
      val field = Field(42, SimpleID("foo"), "foo", structRef)
      resolver(field) must be(field.copy(fieldType = structType))
    }

    "transform a Field with enum constant default" in {
      val field = Field(1, SimpleID("field"), "field", enumRef, Some(IdRHS(Identifier("SomeEnum.FOO"))))
      resolver(field) must be(
        Field(1, SimpleID("field"), "field", enumType, Some(EnumRHS(enum, foo))))
    }

    "transform a Function" in {
      val field = Field(1, SimpleID("foo"), "foo", structRef)
      val ex = Field(2, SimpleID("ex"), "ex", structRef)
      val fun = Function(SimpleID("foo"), "foo", structRef, Seq(field), Seq(ex), None)
      resolver(fun) must be(
        Function(SimpleID("foo"), "foo", resolver(fun.funcType), Seq(resolver(field)), Seq(resolver(ex)), None))
    }

    "transform a TypeDef" in {
      val typedef = Typedef(SimpleID("foo"), enumRef, Map("some" -> "annotation"))
      resolver(typedef, None).definition must be(
        typedef.copy(fieldType = enumType))
    }

    "transform a Struct" in {
      resolver(struct, None).definition must be(struct.copy(fields = struct.fields.map(resolver.apply)))
    }

    "transform an Exception" in {
      resolver(ex, None).definition must be(ex.copy(fields = ex.fields.map(resolver.apply)))
    }

    "transform a Const" in {
      val const = ConstDefinition(SimpleID("foo"), enumRef, IdRHS(Identifier("SomeEnum.FOO")), None)
      resolver(const, None).definition must be(ConstDefinition(SimpleID("foo"), enumType, EnumRHS(enum, foo), None))
    }

    "const definition transitivity" in {
      // this code is ok
      //   const string line = "hi"
      //   const string copy = line
      val line = ConstDefinition(SimpleID("line"), TString, StringLiteral("hi"), None)
      val newResolver = resolver(line, None).resolver
      val copy = ConstDefinition(SimpleID("copy"), TString, IdRHS(SimpleID("line")), None)
      newResolver(copy, None).definition must be(
        ConstDefinition(SimpleID("copy"), TString, StringLiteral("hi"), None))

      // this code has type mismatch
      //   const string line = "hi"
      //   const i32 copy = line
      //
      val copyWrongType = ConstDefinition(SimpleID("copy"), TI32, IdRHS(SimpleID("line")), None)
      intercept[TypeMismatchException] {
        newResolver(copyWrongType, None)
      }

      // this code has undefined symbol
      //   const string line = "hi"
      //   const string copy = noSuchConst
      val copyWrongId = ConstDefinition(SimpleID("copy"), TString, IdRHS(SimpleID("noSuchConst")), None)
      intercept[UndefinedConstantException] {
        newResolver(copyWrongId, None)
      }
    }

    "allow a valid MapRHS for a StructType" in {
      val resolver = TypeResolver()
      val testStruct1 = createStruct("Test1", TI32)
      val testStruct2 = createStruct("Test2", StructType(testStruct1))
      val structType = StructType(testStruct2)
      val mapRHS = MapRHS(Seq((StringLiteral("Test1_field"), IntLiteral(3))))
      val mapRHS1 = MapRHS(Seq((StringLiteral("Test2_field"), mapRHS)))
      val value = resolver(mapRHS1, structType)
      val expectedValue = StructRHS(Map(
        SimpleID("Test2_field") -> StructRHS(Map(SimpleID("Test1_field") -> IntLiteral(3)),Map(SimpleID("Test1_field") -> TI32))
      ),Map(
        SimpleID("Test2_field") -> StructType(Struct(SimpleID("Test1"),"Test1",List(Field(1,SimpleID("Test1_field"),"Test1_field",TI32,None,Requiredness.Default,Map(),Map())),None,Map()),None)
      ))
      value must be(expectedValue)
    }

    "throw a TypeMismatchException if invalid MapRHS passed in for a StructType" in {
      val resolver = TypeResolver()
      val structType = StructType(createStruct("Test1", TString))
      val mapRHS = MapRHS(Seq((StringLiteral("invalid_field"), StringLiteral("Hello"))))
      intercept[TypeMismatchException] {
        resolver(mapRHS, structType)
      }
    }

    "transform a Service" in {
      val fun = Function(SimpleID("foo"), "foo", structRef,
        Seq(Field(1, SimpleID("foo"), "foo", structRef)), Nil, None)
      val service = Service(SimpleID("Glurb"), None, Seq(fun), None)
      resolver(service, None).definition must be(service.copy(functions = Seq(resolver(fun))))
    }

    "resolve a service parent from same scope" in {
      val service1 = Service(SimpleID("Super"), None, Nil, None)
      val service2 = Service(
        SimpleID("Sub"),
        Some(ServiceParent(SimpleID("Super"), None)),
        Nil,
        None)
      val resolver = TypeResolver().withMapping(service1)
      resolver(service2, None).definition must be(service2.copy(parent =
        Some(ServiceParent(
          service1.sid,
          None,
          Some(service1)))))
    }

    "resolve a parameter from an included scope" in {
      val oneInt = Struct(SimpleID("TestRequest"), "TestRequest", Seq(), None, Map.empty)
      val doc = Document(Nil, Seq(oneInt))
      val resolver = TypeResolver().withMapping(Include("other.thrift", doc))
      val resolveFieldType: FieldType = resolver.resolveFieldType(QualifiedID(Seq("other", "TestRequest")))
      resolveFieldType.asInstanceOf[StructType].scopePrefix must be(Some(SimpleID("other")))
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
      resolver(service2, None).definition must be(
        service2.copy(parent = Some(ServiceParent(
          SimpleID("Super"),
          Some(SimpleID("other")),
          Some(service1)))))
    }

    "resolve a typedef from an included scope" in {
      val oneInt = Struct(SimpleID("OneInt"), "OneInt", Seq(Field(1, SimpleID("id"), "id", TI32, None, Requiredness.Default)), None, Map.empty)
      val typedefInt = Typedef(SimpleID("ManyInts"), ListType(ReferenceType(Identifier("OneInt")), None), Map.empty)
      val doc1 = Document(Nil, Seq(oneInt, typedefInt))

      val collectionStruct = Struct(SimpleID("IntCollection"), "IntCollection", Seq(
        Field(1, SimpleID("scores1"), "scores1", ReferenceType(Identifier("typedef1.ManyInts")), None, Requiredness.Default),
        Field(2, SimpleID("scores2"), "scores2", SetType(ReferenceType(Identifier("typedef1.OneInt")), None), None, Requiredness.Default)
      ), None, Map("foo" -> "bar"))
      val doc2 = Document(Seq(Include("src/test/thrift/typedef1.thrift", doc1)), Seq(collectionStruct))

      val resolvedDoc = TypeResolver()(doc2).document
      resolvedDoc.defs(0) match {
        case Struct(_, _, fields, _, annotations) => {
          fields(0) match {
            case Field(1, _, _, ListType(StructType(_, Some(SimpleID("typedef1"))), None), _, _, _, _) => // pass
            case _ => fail()
          }
          fields(1) match {
            case Field(2, _, _, SetType(StructType(_, Some(SimpleID("typedef1"))), None), _, _, _, _) => // pass
            case _ => fail()
          }
          annotations must be(Map("foo" -> "bar"))
        }
        case _ =>
          fail()
      }
    }
  }
}
