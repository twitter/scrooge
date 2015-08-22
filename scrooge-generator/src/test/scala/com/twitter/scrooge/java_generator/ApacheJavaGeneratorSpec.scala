package com.twitter.scrooge.java_generator

import com.github.mustachejava.{DefaultMustacheFactory, Mustache}
import com.google.common.base.Charsets
import com.google.common.io.CharStreams
import com.twitter.scrooge.mustache.ScalaObjectHandler
import com.twitter.scrooge.ast._
import com.twitter.scrooge.frontend.{ResolvedDocument, TypeResolver, _}
import com.twitter.scrooge.testutil.Spec
import com.twitter.scrooge.testutil.Utils.verify
import java.io._
import java.util.EnumSet
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.TMemoryBuffer
import org.mockito.Mockito._
import scala.collection.concurrent.TrieMap
import thrift.apache_java_test._

/**
 * To generate the apache output for birdcage compatible thrift:
 * ~/birdcage/maven-plugins/maven-finagle-thrift-plugin/src/main/resources/thrift/thrift-finagle.osx10.6
 *     --gen java -o /tmp/thrift test_thrift/empty_struct.thrift
 */
class ApacheJavaGeneratorSpec extends Spec {
  def generateDoc(str: String) = {
    val importer = Importer(Seq("src/test/resources/test_thrift", "scrooge-generator/src/test/resources/test_thrift"))
    val parser = new ThriftParser(importer, true)
    val doc = parser.parse(str, parser.document)
    TypeResolver()(doc).document
  }

  val templateCache = new TrieMap[String, Mustache]

  def getGenerator(doc0: Document, genHashcode: Boolean = false) = {
    new ApacheJavaGenerator(
      ResolvedDocument(new Document(Seq(), Seq()), TypeResolver()),
      "thrift",
      templateCache,
      genHashcode = genHashcode
    )
  }

  def getFileContents(resource: String) = {
    val ccl = Thread.currentThread().getContextClassLoader
    val is = ccl.getResourceAsStream(resource)
    val br = new BufferedReader(new InputStreamReader(is, Charsets.UTF_8))
    CharStreams.toString(br)
  }

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

    "generate enum code" in {
      val controller = mock[EnumController]
      when(controller.name) thenReturn "test"
      when(controller.constants) thenReturn Seq(new EnumConstant("foo", 1, false), new EnumConstant("bar", 2, true))
      when(controller.namespace) thenReturn "com.twitter.thrift"
      when(controller.has_namespace) thenReturn true
      val sw = renderMustache("enum.mustache", controller)
      verify(sw, getFileContents("apache_output/enum.txt"))
    }

    "use an EnumSet for a set of enums" in {
      val obj = new StructWithEnumSet()
      obj.getCodes must be(null);

      obj.setCodes(java.util.EnumSet.of(ReturnCode.Good));
      obj.getCodes.isInstanceOf[EnumSet[ReturnCode]] must be(true)
      obj.getCodes.size must be(1)
      obj.getCodesWithDefault.isInstanceOf[EnumSet[ReturnCode]] must be(true)
      obj.getCodesWithDefault.size must be(1)

      val copy = new StructWithEnumSet(obj);
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
      val doc = generateDoc(getFileContents("test_thrift/consts.thrift"))
      val controller = new ConstController(doc.consts, getGenerator(doc), doc.namespace("java"))
      val sw = renderMustache("consts.mustache", controller)
      verify(sw, getFileContents("apache_output/consts.txt"))
    }

    "populate const map" in {
      val doc = generateDoc(getFileContents("test_thrift/constant_map.thrift"))
      val generator = getGenerator(doc, genHashcode = true)
      val controller = new ConstController(doc.consts, generator, doc.namespace("java"))
      val sw = renderMustache("consts.mustache", controller)
      verify(sw, getFileContents("apache_output/constant_map.txt"))
    }

    "generate struct" in {
      val doc = generateDoc(getFileContents("test_thrift/struct.thrift"))
      val controller = new StructController(doc.structs(1), false, getGenerator(doc), doc.namespace("java"))
      val sw = renderMustache("struct.mustache", controller)
      verify(sw, getFileContents("apache_output/struct.txt"))
    }

    "generate struct with hashcode" in {
      val doc = generateDoc(getFileContents("test_thrift/struct.thrift"))
      val generator = getGenerator(doc, genHashcode = true)
      val controller = new StructController(doc.structs(1), false, generator, doc.namespace("java"))
      val sw = renderMustache("struct.mustache", controller)
      verify(sw, getFileContents("apache_output/struct_with_hashcode.txt"))
    }

    "generate empty struct" in {
      val doc = generateDoc(getFileContents("test_thrift/empty_struct.thrift"))
      val controller = new StructController(doc.structs(0), false, getGenerator(doc), doc.namespace("java"))
      val sw = renderMustache("struct.mustache", controller)
      verify(sw, getFileContents("apache_output/empty_struct.txt"), false)
    }

    "generate exception" in {
      val doc = generateDoc(getFileContents("test_thrift/service.thrift"))
      val controller = new StructController(doc.structs(1), false, getGenerator(doc), doc.namespace("java"))
      val sw = renderMustache("struct.mustache", controller)
      verify(sw, getFileContents("apache_output/test_exception.txt"))
    }

    "generate union" in {
      val doc = generateDoc(getFileContents("test_thrift/union.thrift"))
      val controller = new StructController(doc.structs(0), false, getGenerator(doc), doc.namespace("java"))
      val sw = renderMustache("struct.mustache", controller)
      verify(sw, getFileContents("apache_output/union.txt"))
    }

    "generate union with hashcode" in {
      val doc = generateDoc(getFileContents("test_thrift/union.thrift"))
      val generator = getGenerator(doc, genHashcode = true)
      val controller = new StructController(doc.structs(0), false, generator, doc.namespace("java"))
      val sw = renderMustache("struct.mustache", controller)
      verify(sw, getFileContents("apache_output/union_with_hashcode.txt"))
    }

    "generate service that extends parent" in {
      val doc = generateDoc(getFileContents("test_thrift/service.thrift"))
      val controller = new ServiceController(doc.services(1), getGenerator(doc), doc.namespace("java"))
      val sw = renderMustache("service.mustache", controller)
      verify(sw, getFileContents("apache_output/test_service.txt"))
    }

    "generate service that does not extend parent" in {
      val doc = generateDoc(getFileContents("test_thrift/service_without_parent.thrift"))
      val controller = new ServiceController(doc.services(0), getGenerator(doc), doc.namespace("java"))
      val sw = renderMustache("service.mustache", controller)
      verify(sw, getFileContents("apache_output/test_service_without_parent.txt"))
    }

    "generate service with a parent from a different namespace" in {
      val baseDoc = mock[Document]
      val parentDoc = mock[ResolvedDocument]
      when(baseDoc.namespace("java")) thenReturn Some(QualifiedID(Seq("com", "twitter", "thrift")))
      when(parentDoc.document) thenReturn baseDoc
      val doc = generateDoc(getFileContents("test_thrift/service_with_parent_different_namespace.thrift"))
      val generator = new ApacheJavaGenerator(
        ResolvedDocument(new Document(Seq(), Seq()), TypeResolver(
          includeMap = Map("service" -> parentDoc)
        )),
        "thrift",
        templateCache,
        false
      )
      val controller = new ServiceController(doc.services(0), generator, doc.namespace("java"))
      val sw = renderMustache("service.mustache", controller)
      verify(sw, getFileContents("apache_output/other_service.txt"))
    }
  }

  def renderMustache(template: String, controller: Object) = {
    val mf = new DefaultMustacheFactory("apachejavagen/")
    mf.setObjectHandler(new ScalaObjectHandler)
    val m = mf.compile(template)
    val sw = new StringWriter()
    m.execute(sw, controller).flush()
    // Files.write(sw.toString, new File("/tmp/test"), Charsets.UTF_8)
    sw.toString
  }
}
