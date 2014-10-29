package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast._
import com.github.mustachejava.{Mustache, DefaultMustacheFactory}
import com.twitter.mustache.ScalaObjectHandler
import java.io.{FileWriter, File, StringWriter}
import com.twitter.scrooge.ast.EnumType
import com.twitter.scrooge.ast.StructType
import com.twitter.scrooge.ast.SetType
import com.twitter.scrooge.ast.MapType
import com.twitter.scrooge.ast.ListType
import com.twitter.scrooge.ast.ReferenceType
import com.twitter.conversions.string._
import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import com.twitter.scrooge.backend.{ScalaGenerator, GeneratorFactory, ThriftGenerator, ServiceOption}
import com.twitter.scrooge.frontend.{ScroogeInternalException, ResolvedDocument}

object ApacheJavaGeneratorFactory extends GeneratorFactory {
  val lang = "java"
  private val templateCache = new TrieMap[String, Mustache]
  def apply(
    includeMap: Map[String, ResolvedDocument],
    defaultNamespace: String,
    experimentFlags: Seq[String]
  ): ThriftGenerator = new ApacheJavaGenerator(includeMap, defaultNamespace, templateCache)
}


class ApacheJavaGenerator(
    includeMap: Map[String, ResolvedDocument],
    defaultNamespace: String,
    templateCache: TrieMap[String, Mustache],
    val genHashcode: Boolean = true // Defaulting to true for pants.
  ) extends ThriftGenerator {
  var counter = 0

  def printConstValue(
      name: String,
      fieldType: FieldType,
      value: RHS,
      ns: Option[Identifier],
      in_static: Boolean = false,
      defval: Boolean = false) = {
    val controller = new PrintConstController(name, fieldType, value, this, ns, in_static, defval)
    renderMustache("print_const.mustache", controller).trim
  }

  def deepCopyContainer(
      source_name_p1: String,
      source_name_p2: String,
      result_name: String,
      fieldType: FieldType,
      ns: Option[Identifier]) = {
    val controller = new DeepCopyController(
      source_name_p1,
      source_name_p2,
      result_name,
      fieldType,
      this,
      ns)
    renderMustache("generate_deep_copy_container.mustache", controller).trim
  }

  def deepCopyNonContainer(source_name: String, fieldType: FieldType, ns: Option[Identifier]) = {
    val controller = new DeepCopyController(source_name, "", "", fieldType, this, ns)
    renderMustache("generate_deep_copy_noncontainer.mustache", controller).trim
  }

  def deserializeField(fieldType: FieldType, fieldName: String, ns: Option[Identifier], prefix: String = "") = {
    val controller = new DeserializeFieldController(fieldType, fieldName, prefix, this, ns)
    renderMustache("generate_deserialize_field.mustache", controller).trim
  }

  def serializeField(fieldType: FieldType, fieldName: String, ns: Option[Identifier], prefix: String = "") = {
    val controller = new SerializeFieldController(fieldType, fieldName, prefix, this, ns)
    renderMustache("generate_serialize_field.mustache", controller).trim
  }

  def fieldValueMetaData(fieldType: FieldType, ns: Option[Identifier]): String = {
    val controller = new FieldValueMetadataController(fieldType, this, ns)
    renderMustache("generate_field_value_meta_data.mustache", controller).trim
  }

  def renderMustache(template: String, controller: Any = this) = {
    val sw = new StringWriter()
    val mustache = templateCache.getOrElseUpdate(template, {
      val mf = new DefaultMustacheFactory("apachejavagen/")
      mf.setObjectHandler(new ScalaObjectHandler)
      val m = mf.compile(template)
      m
    })
    mustache.execute(sw, controller).flush()
    sw.toString
  }

  def tmp(prefix: String = "tmp") = {
    val tmpVal = prefix + counter
    counter = counter + 1
    tmpVal
  }

  def namespacedFolder(destFolder: File, namespace: String, dryRun: Boolean) = {
    val file = new File(destFolder, namespace.replace('.', File.separatorChar))
    if (!dryRun) file.mkdirs()
    file
  }

  def getNamespace(doc: Document): Identifier = doc.namespace("java") getOrElse SimpleID(defaultNamespace)

  def getIncludeNamespace(includeFileName: String): Identifier = {
    val javaNamespace = includeMap.get(includeFileName).flatMap {
      doc: ResolvedDocument => doc.document.namespace("java")
    }
    javaNamespace.getOrElse(SimpleID(defaultNamespace))
  }

  def qualifyNamedType(sid: SimpleID, scopePrefix: Option[SimpleID]): Identifier =
    scopePrefix match {
      case Some(scope) => sid.addScope(getIncludeNamespace(scope.name))
      case None => sid
    }

  def typeName(
      t: FunctionType,
      inContainer: Boolean = false,
      inInit: Boolean = false,
      skipGeneric: Boolean = false): String = {
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
      case n: NamedType => qualifyNamedType(n.sid, n.scopePrefix).fullName
      case MapType(k, v, _) => {
        val prefix = if (inInit) "HashMap" else "Map"
        prefix + (if (skipGeneric) "" else "<" + typeName(k, inContainer = true) + "," + typeName(v, inContainer = true) + ">")
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

  def getTypeString(fieldType: FunctionType): String = {
    fieldType match {
      case TString => "TType.STRING"
      case TBool => "TType.BOOL"
      case TByte => "TType.BYTE"
      case TI16 => "TType.I16"
      case TI32 => "TType.I32"
      case TI64 => "TType.I64"
      case TDouble => "TType.DOUBLE"
      case EnumType(enumValue, scope) => "TType.I32"
      case StructType(structLike, scope) => "TType.STRUCT"
      case MapType(key, value, cpp) => "TType.MAP"
      case SetType(key, cpp) => "TType.SET"
      case ListType(key, cpp) => "TType.LIST"
      case TBinary => "TType.STRING"
      case _ => {
        throw new ScroogeInternalException("INVALID TYPE IN getTypeString: " + fieldType)
      }
    }
  }

  def isNullableType(t: FunctionType) = {
    t match {
      case TBool | TByte | TI16 | TI32 | TI64 | TDouble => false
      case _ => true
    }
  }

  // main entry
  def apply(doc: Document, serviceOptions: Set[ServiceOption], outputPath: File, dryRun: Boolean = false) = {
    // TODO: Implement serviceOptions (WithFinagle, etc)
    val generatedFiles = new mutable.ListBuffer[File]
    val namespace = getNamespace(doc)
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
      generatedFiles += renderFile("consts.mustache", new ConstController(doc.consts, this, Some(namespace)))
    }

    doc.enums.foreach { enum =>
      generatedFiles += renderFile("enum.mustache", new EnumController(enum, this, Some(namespace)))
    }

    doc.structs.foreach { struct =>
      generatedFiles += renderFile("struct.mustache", new StructController(struct, false, this, Some(namespace)))
    }

    doc.services.foreach { service =>
      generatedFiles += renderFile("service.mustache", new ServiceController(service, this, Some(namespace)))
    }

    generatedFiles
  }
}
