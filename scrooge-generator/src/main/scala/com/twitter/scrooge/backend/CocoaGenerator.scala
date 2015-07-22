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

  def normalizeCase[N <: Node](node: N): N = {
    (node match {
      case d: Document =>
        d.copy(defs = d.defs.map(normalizeCase _))
      case id: Identifier => id.toTitleCase
      case e: EnumRHS =>
        e.copy(normalizeCase(e.enum), normalizeCase(e.value))
      case f: Field =>
        f.copy(
          sid = f.sid.toCamelCase,
          default = f.default.map(normalizeCase _))
      case f: Function =>
        f.copy(
          funcName = f.funcName.toCamelCase,
          args = f.args.map(normalizeCase _),
          throws = f.throws.map(normalizeCase _))
      case c: ConstDefinition =>
        c.copy(value = normalizeCase(c.value))
      case e: Enum =>
        e.copy(values = e.values.map(normalizeCase _))
      case e: EnumField =>
        e.copy(sid = e.sid.toUpperCase)
      case s: Struct =>
        s.copy(fields = s.fields.map(normalizeCase _))
      case f: FunctionArgs =>
        f.copy(fields = f.fields.map(normalizeCase _))
      case f: FunctionResult =>
        f.copy(fields = f.fields.map(normalizeCase _))
      case e: Exception_ =>
        e.copy(fields = e.fields.map(normalizeCase _))
      case s: Service =>
        s.copy(functions = s.functions.map(normalizeCase _))
      case n => n
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
  def genToImmutable(t: FieldType): CodeFragment = codify("")
  def genToImmutable(f: Field): CodeFragment = codify("")
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
    codify(code)
  }

  def genFieldType(f: Field): CodeFragment = {
    codify(genType(f.fieldType).toData)
  }

  // Not used by Cocoa code generator
  def genPrimitiveType(t: FunctionType): CodeFragment = codify("")

  // Not used by Cocoa code generator
  def genFieldParams(fields: Seq[Field], asVal: Boolean = false): CodeFragment = codify("")

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
