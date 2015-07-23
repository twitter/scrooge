package com.twitter.scrooge.backend

import com.twitter.scrooge.ast._
import com.twitter.scrooge.mustache.Dictionary._
import com.twitter.scrooge.mustache.HandlebarLoader
import com.twitter.scrooge.frontend.{ScroogeInternalException, ResolvedDocument}

import java.io.{OutputStreamWriter, FileOutputStream, File}
import scala.collection.mutable

object CocoaGeneratorFactory extends GeneratorFactory {
  val lang = "cocoa"
  val headerTemplateLoader = new HandlebarLoader("/cocoagen/", ".h")
  val implementationTemplateLoader = new HandlebarLoader("/cocoagen/", ".m")
  def apply(
    includeMap: Map[String, ResolvedDocument],
    defaultNamespace: String,
    experimentFlags: Seq[String]
  ): ThriftGenerator = new CocoaGenerator(
    includeMap,
    defaultNamespace,
    headerTemplateLoader,
    implementationTemplateLoader,
    lang)
}

class CocoaGenerator(
  val includeMap: Map[String, ResolvedDocument],
  val defaultNamespace: String,
  val headerTemplateLoader: HandlebarLoader,
  val implementationTemplateLoader: HandlebarLoader,
  val lang: String
) extends CocoaTemplateGenerator {

  val fileExtension = ".m"
  val headerExtension = ".h"
  val templateDirName = "/cocoagen/"
  val experimentFlags = Seq.empty[String]

  // Namespace for the current thrift file is not avaialbe when we construct the code generator.
  // It will only be available when we call the apply method.
  var currentNamespace = ""

  def templates = implementationTemplateLoader

  private[this] object CocoaKeywords {
    private[this] val set = Set[String](
      "auto", "break", "case", "char", "const", "continue", "default", "do", "double",
      "else", "enum", "extern", "float", "for", "goto", "if", "inline", "int", "long",
      "register", "restrict", "return", "short", "signed", "sizeof", "static", "struct",
      "switch", "typedef", "union", "unsigned", "void", "volatile", "while", "_bool",
      "_complex", "_imaginary", "bool", "class", "bycopy", "byref", "id", "imp", "in",
      "inout", "nil", "no", "null", "oneway", "out", "protocol", "sel", "self", "super",
      "yes", "atomic", "nonatomic", "retain", "strong")
    def contains(str: String): Boolean = set.contains(str.toLowerCase)
  }

  def quoteKeyword(str: String): String =
    if (CocoaKeywords.contains(str))
      str + "_"
    else
      str

  override def normalizeCase[N <: Node](node: N): N = {
    (node match {
      case e: EnumField =>
        e.copy(sid = e.sid.toUpperCase)
      case _ => super.normalizeCase(node)
    }).asInstanceOf[N]
  }

  override def getNamespace(doc: Document): Identifier =
    doc.namespace(lang) getOrElse SimpleID(defaultNamespace)

  override def getIncludeNamespace(includeFileName: String): Identifier = {
    val cocoaNamespace = includeMap.get(includeFileName).flatMap {
      doc: ResolvedDocument => doc.document.namespace(lang)
    }
    cocoaNamespace getOrElse SimpleID(defaultNamespace)
  }

  override def isLazyReadEnabled(t: FunctionType, optional: Boolean): Boolean = false

  override def qualifyNamedType(t: NamedType): Identifier =
    t.scopePrefix match {
      case Some(scope) => t.sid.prepend(getIncludeNamespace(scope.name).fullName)
      case None => t.sid.prepend(currentNamespace)
  }

  // For constants support, not implemented yet
  def genList(list: ListRHS, fieldType: Option[FieldType] = None): CodeFragment =
    throw new Exception("not implemented")
  def genSet(set: SetRHS, fieldType: Option[FieldType]): CodeFragment =
    throw new Exception("not implemented")
  def genMap(map: MapRHS, fieldType: Option[FieldType] = None): CodeFragment =
    throw new Exception("not implemented")
  def genEnum(enum: EnumRHS, fieldType: Option[FieldType] = None): CodeFragment =
    throw new Exception("not implemented")
  def genStruct(struct: StructRHS): CodeFragment =
    throw new Exception("not implemented")
  def genUnion(struct: UnionRHS): CodeFragment =
    throw new Exception("not implemented")

  // For mutability/immutability support, not implemented
  def genToImmutable(t: FieldType): CodeFragment = v("")
  def genToImmutable(f: Field): CodeFragment = v("")
  def toMutable(t: FieldType): (String, String) = ("", "")
  def toMutable(f: Field): (String, String) = ("", "")

  def genType(t: FunctionType): CodeFragment = {
    val code = t match {
      case Void => "void"
      case OnewayVoid => "void"
      case TBool => "BOOL"
      case TByte => "int8_t"
      case TI16 => "int16_t"
      case TI32 => "int32_t"
      case TI64 => "int64_t"
      case TDouble => "double"
      case TString => "NSString *"
      case TBinary => "NSData *"
      case MapType(k, v, _) => "NSDictionary *"
      case SetType(x, _) => "NSSet *"
      case ListType(x, _) => "NSArray *"
      case n: NamedType => genID(qualifyNamedType(n).toTitleCase).toData
      case r: ReferenceType =>
        throw new ScroogeInternalException("ReferenceType should not appear in backend")
    }
    v(code)
  }

  def genFieldType(f: Field): CodeFragment = {
    v(genType(f.fieldType).toData)
  }

  // Not used by Cocoa code generator
  def genPrimitiveType(t: FunctionType): CodeFragment = v("")

  // Not used by Cocoa code generator
  def genFieldParams(fields: Seq[Field], asVal: Boolean = false): CodeFragment = v("")

  // Finagle support, not implemented
  def genBaseFinagleService = throw new Exception("not implemented")
  def getParentFinagleService(p: ServiceParent): CodeFragment = throw new Exception("not implemented")
  def getParentFinagleClient(p: ServiceParent): CodeFragment = throw new Exception("not implemented")

  private[this] def writeFile(file: File, fileHeader: String, fileContent: String) {
    val stream = new FileOutputStream(file)
    val writer = new OutputStreamWriter(stream, "UTF-8")
    try {
      writer.write(fileHeader)
      writer.write(fileContent)
    } finally {
      writer.close()
      stream.close()
    }
  }

  override def apply(
    _doc: Document,
    serviceOptions: Set[ServiceOption],
    outputPath: File,
    dryRun: Boolean = false
  ): Iterable[File] = {
    val generatedFiles = new mutable.ListBuffer[File]
    val doc = normalizeCase(_doc)
    val namespace = getNamespace(_doc)
    currentNamespace = namespace.fullName
    val packageDir = outputPath
    val includes = doc.headers.collect {
      case x: Include => x
    }

    val enumsWithNamespace = doc.enums.map(enum => Enum(
      SimpleID(currentNamespace + enum.sid.name),
      enum.values,
      enum.docstring,
      enum.annotations)
    )

    if (!dryRun) {
      enumsWithNamespace.foreach {
        enum =>
          val hFile = new File(packageDir, enum.sid.toTitleCase.name + headerExtension)
          val dict = enumDict(namespace, enum)
          writeFile(hFile, headerTemplateLoader.header, headerTemplateLoader("enum").generate(dict))
      }
    }

    val structsWithNamespace = doc.structs.map(struct => Struct(
      SimpleID(currentNamespace + struct.sid.name),
      struct.originalName,
      struct.fields,
      struct.docstring,
      struct.annotations)
    )

    if (!dryRun) {
      structsWithNamespace.foreach {
        struct =>
          val hFile = new File(packageDir, struct.sid.toTitleCase.name + headerExtension)
          val mFile = new File(packageDir, struct.sid.toTitleCase.name + fileExtension)

          val templateName = "struct"
          val dict = structDict(struct, Some(namespace), includes, serviceOptions)
          writeFile(mFile, implementationTemplateLoader.header, implementationTemplateLoader(templateName).generate(dict))
          writeFile(hFile, headerTemplateLoader.header, headerTemplateLoader(templateName).generate(dict))

          generatedFiles += hFile
          generatedFiles += mFile
      }
    }

    generatedFiles
  }
}
