package com.twitter.scrooge.android_generator

import com.github.mustachejava.{Mustache, DefaultMustacheFactory}
import com.twitter.scrooge.ast.ListType
import com.twitter.scrooge.ast.MapType
import com.twitter.scrooge.ast.ReferenceType
import com.twitter.scrooge.ast.SetType
import com.twitter.scrooge.ast._
import com.twitter.scrooge.backend.{GeneratorFactory, Generator, ServiceOption}
import com.twitter.scrooge.CompilerDefaults
import com.twitter.scrooge.frontend.{ScroogeInternalException, ResolvedDocument}
import com.twitter.scrooge.frontend.ParseException
import com.twitter.scrooge.java_generator.{ApacheJavaGenerator, TypeController}
import com.twitter.scrooge.mustache.ScalaObjectHandler
import java.io.{FileWriter, File, StringWriter}
import scala.collection.concurrent.TrieMap
import scala.collection.mutable

object AndroidGeneratorFactory extends GeneratorFactory {
  val language = "android"
  private val templateCache = new TrieMap[String, Mustache]
  def apply(
    doc: ResolvedDocument,
    defaultNamespace: String,
    experimentFlags: Seq[String]
  ): Generator = new AndroidGenerator(doc, defaultNamespace, templateCache)
}

class AndroidGenerator(
  resolvedDoc: ResolvedDocument,
  defaultNamespace: String,
  templateCache: TrieMap[String, Mustache]
) extends ApacheJavaGenerator(resolvedDoc, defaultNamespace, templateCache) {
  override val namespaceLanguage = "android"

  override def renderMustache(template: String, controller: Any = this) = {
    val sw = new StringWriter()
    val mustache = templateCache.getOrElseUpdate(template, {
      val mf = new DefaultMustacheFactory("androidgen/")
      mf.setObjectHandler(new ScalaObjectHandler)
      val m = mf.compile(template)
      m
    })
    mustache.execute(sw, controller).flush()
    sw.toString
  }

  override def getNamespace(doc: Document): Identifier = doc.namespace("android") getOrElse {
    if (defaultNamespace == CompilerDefaults.defaultNamespace)
      throw new ParseException(
        "You must specify an android namespace in your thrift " +
          "(eg: #@namespace android name.space.here) " +
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
      case Void => if (inContainer) "Void" else "void"
      case OnewayVoid => if (inContainer) "Void" else "void"
      case TBool => if (inContainer) "Boolean" else "boolean"
      case TByte => if (inContainer) "Byte" else "byte"
      case TI16 => if (inContainer) "Short" else "short"
      case TI32 => if (inContainer) "Integer" else "int"
      case TI64 => if (inContainer) "Long" else "long"
      case TDouble => if (inContainer) "Double" else "double"
      case TString => "String"
      case TBinary => "ByteBuffer"
      case n: NamedType =>
        qualifyNamedType(n.sid, n.scopePrefix, fileNamespace).fullName
      case MapType(k, v, _) => {
        val prefix = if (inInit) "HashMap" else "Map"
        prefix + (if (skipGeneric) ""
                  else
                    "<" + typeName(k, inContainer = true) + "," + typeName(v, inContainer = true) + ">")
      }
      case SetType(x, _) => {
        val prefix = if (inInit) "HashSet" else "Set"
        prefix + (if (skipGeneric) "" else "<" + typeName(x, inContainer = true) + ">")
      }
      case ListType(x, _) => {
        val prefix = if (inInit) "ArrayList" else "List"
        prefix + (if (skipGeneric) "" else "<" + typeName(x, inContainer = true) + ">")
      }
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

  def isListOrSetType(t: FunctionType) = {
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

    def renderFile(templateName: String, controller: TypeController) = {
      val fileContent = renderMustache(templateName, controller)
      val file = new File(packageDir, controller.name + ".java")
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
        new ConstController(doc.consts, this, Some(namespace))
      )
    }

    doc.enums.foreach { enum =>
      generatedFiles += renderFile("enum.mustache", new EnumController(enum, this, Some(namespace)))
    }

    doc.structs.foreach { struct =>
      generatedFiles += renderFile(
        "struct.mustache",
        new StructController(struct, false, this, Some(namespace))
      )
    }

    generatedFiles
  }
}
