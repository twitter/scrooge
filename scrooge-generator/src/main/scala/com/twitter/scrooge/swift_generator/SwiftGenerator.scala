package com.twitter.scrooge.swift_generator

import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.Mustache
import com.twitter.scrooge.ast.ListType
import com.twitter.scrooge.ast.MapType
import com.twitter.scrooge.ast.ReferenceType
import com.twitter.scrooge.ast.SetType
import com.twitter.scrooge.ast._
import com.twitter.scrooge.backend.Generator
import com.twitter.scrooge.backend.GeneratorFactory
import com.twitter.scrooge.backend.ServiceOption
import com.twitter.scrooge.CompilerDefaults
import com.twitter.scrooge.frontend.ResolvedDocument
import com.twitter.scrooge.frontend.ScroogeInternalException
import com.twitter.scrooge.frontend.ParseException
import com.twitter.scrooge.java_generator.ApacheJavaGenerator
import com.twitter.scrooge.java_generator.TypeController
import com.twitter.scrooge.mustache.ScalaObjectHandler
import com.twitter.scrooge.mustache.HandlebarLoader
import java.io.File
import java.io.FileWriter
import java.io.StringWriter
import scala.collection.concurrent.TrieMap
import scala.collection.mutable

object SwiftGeneratorFactory extends GeneratorFactory {
  val language = "swift"
  private val templateCache = new TrieMap[String, Mustache]
  def apply(
    doc: ResolvedDocument,
    defaultNamespace: String,
    languageFlags: Seq[String]
  ): Generator = new SwiftGenerator(doc, defaultNamespace, templateCache, languageFlags)
}

class SwiftGenerator(
  resolvedDoc: ResolvedDocument,
  defaultNamespace: String,
  templateCache: TrieMap[String, Mustache],
  languageFlags: Seq[String])
    extends ApacheJavaGenerator(resolvedDoc, defaultNamespace, templateCache) {
  override val namespaceLanguage = "swift"

  private val objcNamePattern = "swift-objc ([A-Za-z]*)".r
  val handlebarLoader = new HandlebarLoader("", ".mustache")

  def objcPrefix: Option[String] = languageFlags.find(_.contains("swift-objc")).flatMap { s =>
    val prefix = objcNamePattern.findFirstMatchIn(s).flatMap { m =>
      Some(m.group(1))
    }
    if (prefix.isEmpty) {
      throw new Exception(
        "Objective-C Prefix not found, insure properly quoted. (Example 'swift-objc TEST')")
    }
    prefix
  }

  var generateClasses: Boolean = languageFlags.exists(_.contains("classes"))

  var publicInterface: Boolean = !languageFlags.exists(_.contains("internal-types"))

  override def renderMustache(template: String, controller: Any = this): String = {
    val sw = new StringWriter()
    val mustache = templateCache.getOrElseUpdate(
      template, {
        val mf = new DefaultMustacheFactory("swiftgen")
        mf.setObjectHandler(new ScalaObjectHandler)
        val m = mf.compile(template)
        m
      }
    )
    mustache.execute(sw, controller).flush()
    sw.toString
  }

  def printConstValue(
    name: String,
    fieldType: FieldType,
    value: RHS,
    ns: Option[Identifier],
    total: Int,
    defval: Boolean
  ): String = {
    val controller =
      new SwiftPrintConstController(
        name,
        fieldType,
        value,
        this,
        ns,
        total,
        defval = defval,
        public_interface = publicInterface)
    renderMustache("print_const.mustache", controller).trim
  }

  override def getNamespace(doc: Document): Identifier = doc.namespace("swift") getOrElse {
    if (defaultNamespace == CompilerDefaults.defaultNamespace)
      throw new ParseException(
        "You must specify an swift namespace in your thrift " +
          "(eg: #@namespace swift name.space.here) " +
          "or use the --default-java-namespace to specify a default namespace"
      )
    SimpleID(defaultNamespace)
  }

  /**
   * @param fileNamespace The namespace to add to named types if they are defined in the file being
   *                      generated.
   */
  override def typeName(
    t: FunctionType,
    inContainer: Boolean = false,
    inInit: Boolean = false,
    skipGeneric: Boolean = false,
    fileNamespace: Option[Identifier] = None
  ): String = {
    t match {
      case Void => "Void"
      case OnewayVoid => "Void"
      case TBool => "Bool"
      case TByte => "UInt8"
      case TI16 => "Int16"
      case TI32 => "Int32"
      case TI64 => "Int64"
      case TDouble => "Double"
      case TString => "String"
      case TBinary => "Data"
      case n: NamedType =>
        qualifyNamedType(n.sid, n.scopePrefix, fileNamespace).fullName
      case MapType(k, v, _) => "[" + typeName(k) + ":" + typeName(v) + "]"
      case SetType(x, _) => {
        val prefix = "Set"
        prefix + ("<" + typeName(x) + ">")
      }
      case ListType(x, _) => "[" + typeName(x) + "]"
      case r: ReferenceType =>
        throw new ScroogeInternalException("ReferenceType should not appear in backend")
      case _ => throw new ScroogeInternalException("unknown type")
    }
  }

  def leftElementTypeName(t: FunctionType, skipGeneric: Boolean = false): String = {
    t match {
      case MapType(k, v, _) => typeName(k, inContainer = true, skipGeneric = skipGeneric)
      case SetType(x, _) => typeName(x, inContainer = true, skipGeneric = skipGeneric)
      case ListType(x, _) => typeName(x, inContainer = true, skipGeneric = skipGeneric)
      case _ => ""
    }
  }

  def rightElementTypeName(t: FunctionType, skipGeneric: Boolean = false): String = {
    t match {
      case MapType(k, v, _) => typeName(v, inContainer = true, skipGeneric = skipGeneric)
      case _ => ""
    }
  }

  def isListOrSetType(t: FunctionType): Boolean = {
    t match {
      case ListType(_, _) => true
      case SetType(_, _) => true
      case _ => false
    }
  }

  override def apply(
    serviceOptions: Set[ServiceOption],
    outputPath: File,
    dryRun: Boolean = false,
    genAdapt: Boolean = false
  ): Iterable[File] = {
    val generatedFiles = new mutable.ListBuffer[File]
    val packageDir = namespacedFolder(outputPath, namespace.fullName, dryRun)
    val sourceDir = namespacedFolder(packageDir, "Sources/" + namespace.fullName, dryRun)

    def renderFile(
      templateName: String,
      controller: TypeController,
      fileName: Option[String] = None
    ) = {
      val dir = if (templateName == "package_file.mustache") packageDir else sourceDir
      val fileContent = renderMustache(templateName, controller)
      val name = fileName.getOrElse(controller.name)
      val file = new File(dir, name + ".swift")
      if (!dryRun) {
        val writer = new FileWriter(file)
        try {
          writer.write(fileContent)
        } finally {
          writer.close()
        }
      }
      file
    }

    if (doc.consts.nonEmpty) {
      generatedFiles += renderFile(
        "consts.mustache",
        new ConstController(
          doc.consts,
          this,
          Some(namespace),
          public_interface = publicInterface
        )
      )
    }

    generatedFiles += renderFile(
      "package_file.mustache",
      new PackageFileController(doc.consts, this, Some(namespace))
    )

    doc.enums.foreach { enum =>
      generatedFiles += renderFile(
        "enum.mustache",
        new EnumController(
          enum,
          this,
          Some(namespace),
          objcPrefix = objcPrefix,
          public_interface = publicInterface),
        enum.annotations.get("alternative.type")
      )
    }

    doc.structs.foreach { struct =>
      generatedFiles += renderFile(
        "struct.mustache",
        new StructController(
          struct,
          false,
          this,
          Some(namespace),
          objcPrefix = objcPrefix,
          use_classes = generateClasses,
          public_interface = publicInterface),
        struct.annotations.get("alternative.type")
      )
    }

    generatedFiles
  }
}
