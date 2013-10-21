package com.twitter.scrooge.java_generator

import org.specs.SpecificationWithJUnit
import com.twitter.scrooge.frontend._
import java.io._
import com.github.mustachejava.DefaultMustacheFactory
import com.twitter.mustache.ScalaObjectHandler
import org.specs.mock.Mockito
import com.google.common.base.Charsets
import com.google.common.io.CharStreams
import com.twitter.scrooge.ast._
import com.twitter.scrooge.java_generator.test.ApacheCompatibilityHelpers
import com.twitter.scrooge.frontend.{ResolvedDocument, TypeResolver}
import com.twitter.scrooge.ast.Document

/**
 * To generate the apache output for birdcage compatible thrift:
 * ~/birdcage/maven-plugins/maven-finagle-thrift-plugin/src/main/resources/thrift/thrift-finagle.osx10.6
 *     --gen java -o /tmp/thrift test_thrift/empty_struct.thrift
 */
class ApacheJavaGeneratorSpec extends SpecificationWithJUnit with Mockito {
  def generateDoc(str: String) = {
    val importer = Importer(Seq("src/test/resources/test_thrift", "scrooge-generator/src/test/resources/test_thrift"))
    val parser = new ThriftParser(importer, true)
    val doc = parser.parse(str, parser.document)
    TypeResolver(allowStructRHS = true)(doc).document
  }

  def getGenerator(doc0: Document, genHashcode: Boolean = false) = {
    new ApacheJavaGenerator(Map(), "thrift", genHashcode = genHashcode)
  }

  def getFileContents(resource: String) = {
    val ccl = Thread.currentThread().getContextClassLoader
    val is = ccl.getResourceAsStream(resource)
    val br = new BufferedReader(new InputStreamReader(is, Charsets.UTF_8))
    CharStreams.toString(br)
  }

  "Generator"should {
    System.setProperty("mustache.debug", "true")

    "populate enum controller" in {
      val doc = generateDoc(getFileContents("test_thrift/enum.thrift"))
      val controller = new EnumController(doc.enums(0), getGenerator(doc), doc.namespace("java"))
      controller.name must_== "test"
      controller.constants(0).last must_== false
      controller.constants(1).last must_== true
      controller.constants(0).name must_== "foo"
      controller.namespace must_== "com.twitter.thrift"
    }

    "generate enum code" in {
      val controller = mock[EnumController]
      controller.name returns "test"
      controller.constants returns Seq(new EnumConstant("foo", 1, false), new EnumConstant("bar", 2, true))
      controller.namespace returns "com.twitter.thrift"
      controller.has_namespace returns true
      val sw = renderMustache("enum.mustache", controller)
      verify(sw, getFileContents("apache_output/enum.txt"))
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
      doReturn(Some(QualifiedID(Seq("com", "twitter", "thrift")))).when(baseDoc).namespace("java")
      doReturn(baseDoc).when(parentDoc).document
      val doc = generateDoc(getFileContents("test_thrift/service_with_parent_different_namespace.thrift"))
      val generator = new ApacheJavaGenerator(Map("service" -> parentDoc), "thrift", false)
      val controller = new ServiceController(doc.services(0), generator, doc.namespace("java"))
      val sw = renderMustache("service.mustache", controller)
      verify(sw, getFileContents("apache_output/other_service.txt"))
    }
  }

  def verify(actual: String, expected: String, cleanEmptySemicolons: Boolean = true) {
    val actualItems = ApacheCompatibilityHelpers.cleanWhitespace(actual, cleanEmptySemicolons)
    val expectedItems = ApacheCompatibilityHelpers.cleanWhitespace(expected, cleanEmptySemicolons)
    for (i <- 0 until actualItems.size) {
      if (!actualItems(i).equals(expectedItems(i))) {
        println("Actual: " + actualItems(i))
        println("Expected: " + expectedItems(i))
        // No idea why the one below doesn't make the test red
        assert(actualItems(i) == expectedItems(i))
      } else {
        // println(expectedItems(i))
      }
      actualItems(i) must_== expectedItems(i)
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
