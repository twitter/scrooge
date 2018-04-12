package com.twitter.scrooge.java_generator

import apache_java_thrift.{ConstructorRequiredStruct, DeepValidationStruct, DeepValidationUnion}
import com.github.mustachejava.{DefaultMustacheFactory, Mustache}
import com.twitter.scrooge.CompilerDefaults
import com.twitter.scrooge.ast._
import com.twitter.scrooge.frontend.{ResolvedDocument, TypeResolver, _}
import com.twitter.scrooge.mustache.ScalaObjectHandler
import com.twitter.scrooge.testutil.Spec
import com.twitter.scrooge.testutil.Utils.{verifyWithHint, getFileContents}
import com.twitter.util.Try
import java.io._
import java.util.{EnumSet, List => JList, Map => JMap, Set => JSet}
import org.apache.thrift.protocol.{TBinaryProtocol, TProtocolException}
import org.apache.thrift.transport.TMemoryBuffer
import org.mockito.Mockito._
import scala.collection.JavaConverters._
import scala.collection.concurrent.TrieMap
import thrift.apache_java_test._

/**
 * To generate the apache output for birdcage compatible thrift:
 * ~/birdcage/maven-plugins/maven-finagle-thrift-plugin/src/main/resources/thrift/thrift-finagle.osx10.6
 *     --gen java -o /tmp/thrift test_thrift/empty_struct.thrift
 */
class ApacheJavaGeneratorSpec extends Spec {
  object TestThriftResourceImporter extends Importer {
    val canonicalPaths: Seq[String] = Nil
    def lastModified(filename: String): Option[Long] = None
    def apply(filename: String): Option[FileContents] =
      Try(getFileContents("test_thrift/" + filename))
        .map(data => FileContents(this, data, Some(filename)))
        .toOption
  }

  def generateDoc(str: String): Document = {
    val importer = TestThriftResourceImporter
    val parser = new ThriftParser(importer, true)
    val doc = parser.parse(str, parser.document)
    TypeResolver()(doc).document
  }

  val templateCache = new TrieMap[String, Mustache]

  def getGenerator(doc0: Document): ApacheJavaGenerator =
    new ApacheJavaGenerator(
      ResolvedDocument(doc0, TypeResolver()),
      "thrift",
      templateCache
    )

  "Generator" should {
    System.setProperty("mustache.debug", "true")

    "populate enum controller" in {
      val doc = generateDoc(getFileContents("test_thrift/enum.thrift"))
      val controller = new EnumController(doc.enums(0), getGenerator(doc), doc.namespace("java"))
      controller.name must be("test")
      controller.constants(0).last must be(false)
      controller.constants(1).last must be(true)
      controller.constants(0).name must be("foo")
      controller.namespace must be("com.twitter.thrift")
    }

    "use an EnumSet for a set of enums" in {
      val obj = new StructWithEnumSet()
      obj.getCodes must be(null)

      obj.setCodes(java.util.EnumSet.of(ReturnCode.Good))
      obj.getCodes.isInstanceOf[EnumSet[ReturnCode]] must be(true)
      obj.getCodes.size must be(1)
      obj.getCodesWithDefault.isInstanceOf[EnumSet[ReturnCode]] must be(true)
      obj.getCodesWithDefault.size must be(1)

      val copy = new StructWithEnumSet(obj)
      copy.getCodes.isInstanceOf[EnumSet[ReturnCode]] must be(true)
      copy.getCodes.size must be(1)
      copy.getCodesWithDefault.isInstanceOf[EnumSet[ReturnCode]] must be(true)
      copy.getCodesWithDefault.size must be(1)

      val prot = new TBinaryProtocol(new TMemoryBuffer(64))
      obj.write(prot)
      val decoded = new StructWithEnumSet()
      decoded.read(prot)
      decoded.getCodes.isInstanceOf[EnumSet[ReturnCode]] must be(true)
    }

    "populate consts" in {
      val thriftSource = "test_thrift/consts.thrift"
      val doc = generateDoc(getFileContents(thriftSource))
      val controller = new ConstController(doc.consts, getGenerator(doc), getNamespace(doc))
      val sw = renderMustache("consts.mustache", controller)
      verifyWithHint(sw, "apache_output/consts.txt", thriftSource, "com/twitter/thrift/Constants.java", "java")
    }

    "populate const map" in {
      val thriftSource = "test_thrift/constant_map.thrift"
      val doc = generateDoc(getFileContents(thriftSource))
      val generator = getGenerator(doc)
      val controller = new ConstController(doc.consts, generator, getNamespace(doc))
      val sw = renderMustache("consts.mustache", controller)
      verifyWithHint(sw, "apache_output/constant_map.txt", thriftSource, "com/twitter/adserver/Constants.java", "java")
    }

    "generate empty struct" in {
      val thriftSource = "test_thrift/empty_struct.thrift"
      val doc = generateDoc(getFileContents(thriftSource))
      val controller =
        new StructController(doc.structs(0), false, getGenerator(doc), getNamespace(doc))
      val sw = renderMustache("struct.mustache", controller)
      verifyWithHint(sw, "apache_output/empty_struct.txt", thriftSource, "thrift/FollowerTargetingDetails.java", "java")
    }

    "generate struct" in {
      val thriftSource = "test_thrift/struct.thrift"
      val doc = generateDoc(getFileContents(thriftSource))
      val generator = getGenerator(doc)
      val controller = new StructController(doc.structs(1), false, generator, getNamespace(doc))
      val sw = renderMustache("struct.mustache", controller)
      verifyWithHint(sw, "apache_output/struct.txt", thriftSource, "thrift/Work.java", "java")
    }

    "generate union" in {
      val thriftSource = "test_thrift/union_with_enum.thrift"
      val doc = generateDoc(getFileContents(thriftSource))
      val generator = getGenerator(doc)
      val controller = new StructController(doc.structs(0), false, generator, getNamespace(doc))
      val sw = renderMustache("struct.mustache", controller)
      verifyWithHint(sw, "apache_output/union.txt", thriftSource, "thrift/TestUnion.java", "java")
    }

    "generate service that extends parent" in {
      val thriftSource = "test_thrift/service.thrift"
      val doc = generateDoc(getFileContents(thriftSource))
      val controller =
        new ServiceController(doc.services(1), getGenerator(doc), getNamespace(doc))
      val sw = renderMustache("service.mustache", controller)
      verifyWithHint(sw, "apache_output/test_service.txt", thriftSource, "com/twitter/thrift/TestService.java", "java")
    }

    "generate service that does not extend parent" in {
      val thriftSource = "test_thrift/service_without_parent.thrift"
      val doc = generateDoc(getFileContents(thriftSource))
      val controller =
        new ServiceController(doc.services(0), getGenerator(doc), getNamespace(doc))
      val sw = renderMustache("service.mustache", controller)
      verifyWithHint(sw, "apache_output/test_service_without_parent.txt", thriftSource, "com/twitter/thrift/TestService.java", "java")
    }

    "generate service with a parent from a different namespace" in {
      val thriftSource = "test_thrift/service_with_parent_different_namespace.thrift"
      val doc =
        generateDoc(getFileContents(thriftSource))
      val baseDoc = mock[Document]
      val parentDoc = mock[ResolvedDocument]
      when(baseDoc.namespace("java")) thenReturn Some(QualifiedID(Seq("com", "twitter", "thrift")))
      when(parentDoc.document) thenReturn baseDoc
      val generator = new ApacheJavaGenerator(
        ResolvedDocument(
          Document(Seq.empty, Seq.empty),
          TypeResolver(
            includeMap = Map("service" -> parentDoc)
          )
        ),
        "thrift",
        templateCache
      )
      val controller = new ServiceController(doc.services(0), generator, getNamespace(doc))
      val sw = renderMustache("service.mustache", controller)
      verifyWithHint(sw, "apache_output/other_service.txt", thriftSource, "com/twitter/other/OtherService.java", "java")
    }

    "constructor required fields" should {
      "not be optional in the contructor with args" in {
        val struct = new ConstructorRequiredStruct(
          "test",
          3,
          4
        )
        struct.getConstructionRequiredField must be(3)
        struct.isSetConstructionRequiredField must be(true)
      }
    }

    "validate" should {
      "throw an exception when missing a field" in {
        val struct = new ConstructorRequiredStruct(
          null,
          3,
          4
        )
        val exception = intercept[TProtocolException](struct.validate())
        val expected = "Required field 'requiredField' was not present! Struct: " + struct.toString
        exception.getMessage must be(expected)
      }
      "not throw when required fields are present" in {
        val struct = new ConstructorRequiredStruct(
          "present",
          3,
          4
        )
        struct.validate()
      }
    }

    "validateNewInstance" should {
      val validInstance =
        new ConstructorRequiredStruct()
          .setOptionalField(1)
          .setRequiredField("test")
          .setConstructionRequiredField(3)
          .setDefaultRequirednessField(4)

      val missingRequiredFieldInstance =
        new ConstructorRequiredStruct()
          .setOptionalField(1)
          .setConstructionRequiredField(3)
          .setDefaultRequirednessField(4)

      val missingConstructionRequiredFieldInstance =
        new ConstructorRequiredStruct()
          .setOptionalField(1)
          .setRequiredField("test")
          .setDefaultRequirednessField(4)

      val constructionRequiredErrorMessage = "Construction required field 'constructionRequiredField' in type 'ConstructorRequiredStruct' was not present."
      val requiredErrorMessage = "Required field 'requiredField' in type 'ConstructorRequiredStruct' was not present."

      def validateMissingConstructionRequiredField(
        struct: DeepValidationStruct, number: Int = 1
      ): Unit = {
        val result = DeepValidationStruct.validateNewInstance(struct).asScala
        result must have size number
        result.foreach{ errorMessage =>
          errorMessage must be(constructionRequiredErrorMessage)
        }
      }

      def buildDeepValidationStruct(
        listField: JList[ConstructorRequiredStruct] = Seq(validInstance).asJava,
        setField: JSet[ConstructorRequiredStruct] = Set(validInstance).asJava,
        requiredField: ConstructorRequiredStruct = validInstance,
        inMapKey: JMap[ConstructorRequiredStruct, String] = Map(validInstance -> "value").asJava,
        inMapValue: JMap[String, ConstructorRequiredStruct] = Map("value" -> validInstance).asJava,
        crazyEmbedding: JMap[JSet[JList[ConstructorRequiredStruct]], JSet[JList[ConstructorRequiredStruct]]] =
          Map(
            Set(Seq(validInstance).asJava).asJava ->
              Set(Seq(validInstance).asJava).asJava
          ).asJava,
        optionalField: ConstructorRequiredStruct = validInstance
      ): DeepValidationStruct = {
        new DeepValidationStruct(
          listField,
          setField,
          requiredField,
          inMapKey,
          inMapValue,
          crazyEmbedding
        )
          .setOptionalConstructorRequiredStruct(optionalField)
      }

      "return an empty list when required fields are present" in {
        val result = ConstructorRequiredStruct.validateNewInstance(validInstance).asScala
        result must be(List.empty)
      }
      "return a MissingRequiredField when missing a required field" in {
        val result = ConstructorRequiredStruct.validateNewInstance(missingRequiredFieldInstance).asScala
        result must be(List(requiredErrorMessage))
      }
      "return a MissingConstructionRequiredField when missing a required field" in {
        val result = ConstructorRequiredStruct.validateNewInstance(missingConstructionRequiredFieldInstance).asScala
        result must be(List(constructionRequiredErrorMessage))
      }

      "return an empty list when DeepValidationStruct is completely valid" in {
        val struct = buildDeepValidationStruct()
        val result = DeepValidationStruct.validateNewInstance(struct).asScala
        result must be(List.empty)
      }
      "return a MissingConstructionRequiredField when DeepValidationStruct has invalid requiredConstructorRequiredStruct" in {
        val struct = buildDeepValidationStruct(requiredField = missingConstructionRequiredFieldInstance)
        validateMissingConstructionRequiredField(struct)
      }
      "return a MissingConstructionRequiredField when DeepValidationStruct has invalid optionalConstructorRequiredStruct" in {
        val struct = buildDeepValidationStruct(
          optionalField = missingConstructionRequiredFieldInstance
        )
        validateMissingConstructionRequiredField(struct)
      }
      "return a MissingConstructionRequiredField when DeepValidationStruct has invalid inList" in {
        val struct = buildDeepValidationStruct(
          listField = List(missingConstructionRequiredFieldInstance).asJava
        )
        validateMissingConstructionRequiredField(struct)
      }
      "return a MissingConstructionRequiredField when DeepValidationStruct has invalid inSet" in {
        val struct = buildDeepValidationStruct(
          setField = Set(missingConstructionRequiredFieldInstance).asJava
        )
        validateMissingConstructionRequiredField(struct)
      }
      "return a MissingConstructionRequiredField when DeepValidationStruct has invalid inMapKey" in {
        val struct = buildDeepValidationStruct(
          inMapKey = Map(missingConstructionRequiredFieldInstance -> "value").asJava
        )
        validateMissingConstructionRequiredField(struct)
      }
      "return a MissingConstructionRequiredField when DeepValidationStruct has invalid inMapValue" in {
        val struct = buildDeepValidationStruct(
          inMapValue = Map("key" -> missingConstructionRequiredFieldInstance).asJava
        )
        validateMissingConstructionRequiredField(struct)
      }
      "return a MissingConstructionRequiredField when DeepValidationStruct has invalid crazyEmbedding" in {
        val struct = buildDeepValidationStruct(
          crazyEmbedding = Map(
            Set(Seq(missingConstructionRequiredFieldInstance).asJava).asJava ->
              Set(Seq(missingConstructionRequiredFieldInstance).asJava).asJava
          ).asJava
        )
        validateMissingConstructionRequiredField(struct, 2)
      }
      "return an empty list when DeepValidationUnion has valid constructorRequiredStruct" in {
        val struct = new DeepValidationUnion()
        struct.setConstructorRequiredStruct(validInstance)
        val result = DeepValidationUnion.validateNewInstance(struct).asScala
        result must be(List.empty)
      }
      "return a MissingConstructionRequiredField when DeepValidationUnion has invalid constructorRequiredStruct" in {
        val struct = new DeepValidationUnion()
        struct.setConstructorRequiredStruct(missingConstructionRequiredFieldInstance)
        val result = DeepValidationUnion.validateNewInstance(struct).asScala
        result must be(List(constructionRequiredErrorMessage))
      }
      "return an empty list when DeepValidationUnion has an unrelated field" in {
        val struct = new DeepValidationUnion()
        struct.setOtherField(1L)
        val result = DeepValidationUnion.validateNewInstance(struct).asScala
        result must be(List.empty)
      }
      "return an error when DeepValidationUnion has no field set" in {
        val struct = new DeepValidationUnion()
        val result = DeepValidationUnion.validateNewInstance(struct).asScala
        result must be(List("No fields set for union type 'DeepValidationUnion'."))
      }
    }
  }

  private[this] def getNamespace(doc: Document): Some[Identifier] = {
    Some(doc.namespace("java").getOrElse(SimpleID(CompilerDefaults.defaultNamespace)))
  }

  def renderMustache(template: String, controller: Object): String = {
    val mf = new DefaultMustacheFactory("apachejavagen/")
    mf.setObjectHandler(new ScalaObjectHandler)
    val m = mf.compile(template)
    val sw = new StringWriter()
    m.execute(sw, controller).flush()
    // Files.write(sw.toString, new File("/tmp/test"), Charsets.UTF_8)
    sw.toString
  }
}
