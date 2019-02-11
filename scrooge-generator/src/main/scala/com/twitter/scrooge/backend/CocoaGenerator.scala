package com.twitter.scrooge.backend

import com.twitter.scrooge.ast._
import com.twitter.scrooge.mustache.Dictionary._
import com.twitter.scrooge.mustache.{Dictionary, HandlebarLoader}
import com.twitter.scrooge.frontend.{ScroogeInternalException, ResolvedDocument}

import java.io.{OutputStreamWriter, FileOutputStream, File}
import scala.collection.mutable

object CocoaGeneratorFactory extends GeneratorFactory {
  val language = "cocoa"
  val headerTemplateLoader = new HandlebarLoader("/cocoagen/", ".h")
  val implementationTemplateLoader = new HandlebarLoader("/cocoagen/", ".m")
  def apply(
    doc: ResolvedDocument,
    defaultNamespace: String,
    experimentFlags: Seq[String]
  ): Generator = new CocoaGenerator(
    doc,
    defaultNamespace,
    headerTemplateLoader,
    implementationTemplateLoader
  )
}

class CocoaGenerator(
  val doc: ResolvedDocument,
  val defaultNamespace: String,
  val headerTemplateLoader: HandlebarLoader,
  val implementationTemplateLoader: HandlebarLoader)
    extends TemplateGenerator(doc) {

  val namespaceLanguage = "cocoa"

  val fileExtension = ".m"
  val headerExtension = ".h"
  val templateDirName = "/cocoagen/"
  val experimentFlags = Seq.empty[String]

  // Namespace for the current thrift file is not avaialbe when we construct the code generator.
  // It will only be available when we call the apply method.
  var currentNamespace = ""

  def templates = implementationTemplateLoader

  implicit class RichDictionary(dictionary: Dictionary) {
    def update(keyPath: Seq[String], data: String): Unit = {
      keyPath match {
        case head :: Nil => dictionary(head) = data
        case head :: tail => dictionary(head).children.head(tail) = data
        case _ =>
      }
    }

    def update(keyPath: Seq[String], data: Boolean): Unit = {
      keyPath match {
        case head :: Nil => dictionary(head) = data
        case head :: tail => dictionary(head).children.head(tail) = data
        case _ =>
      }
    }
  }

  def getFieldNSCoderMethod(f: Field, isDecode: Boolean = false): String = {
    val prefix = if (isDecode) "decode" else "encode"
    val suffix = if (isDecode) "ForKey" else ""
    val code = f.fieldType match {
      case TBool => "Bool"
      case TByte => "Int32"
      case TI16 => "Int32"
      case TI32 => "Int32"
      case TI64 => "Int64"
      case TDouble => "Double"
      case TBinary => "DataObject"
      case EnumType(_, _) => "Int32"
      case _ => "Object"
    }
    prefix + code + suffix
  }

  def getTypeValueMethod(t: FieldType, name: String): String = {
    val code = t match {
      case TBool => "[%s boolValue]"
      case TByte => "[%s intValue]"
      case TI16 => "[%s intValue]"
      case TI32 => "[%s intValue]"
      case TI64 => "[%s longLongValue]"
      case TDouble => "[%s doubleValue]"
      case EnumType(_, _) => "[%s intValue]"
      case _ => "%s"
    }
    code format name
  }

  def getDependentTypes(struct: StructLike): Set[FieldType] = {
    def getDependentTypes(fieldType: FieldType): Set[FieldType] = {
      fieldType match {
        case t: ListType => getDependentTypes(t.eltType)
        case t: MapType => getDependentTypes(t.keyType) ++ getDependentTypes(t.valueType)
        case t: SetType => getDependentTypes(t.eltType)
        case StructType(_, _) => Set(fieldType)
        case EnumType(_, _) => Set(fieldType)
        case _ => Set()
      }
    }

    struct.fields
      .map(field => getDependentTypes(field.fieldType)).foldLeft(Set[FieldType]())(_ ++ _)
  }

  def getDependentHeaders(struct: StructLike): String = {
    getDependentTypes(struct)
      .map(t => s"""#import <${currentNamespace}/${genType(t).toString}.h>""")
      .toList
      .sorted
      .mkString("\n")
  }

  override def readWriteInfo[T <: FieldType](sid: SimpleID, t: FieldType): Dictionary = {
    val dictionary = super.readWriteInfo(sid, t)
    t match {
      case t: MapType => {
        dictionary(Seq("isMap", "isKeyPrimitive")) = isPrimitive(t.keyType) || t.keyType
          .isInstanceOf[EnumType]
        dictionary(Seq("isMap", "keyGetValueMethod")) =
          getTypeValueMethod(t.keyType, genID(sid) + "_key_id")
        dictionary(Seq("isMap", "valueGetValueMethod")) =
          getTypeValueMethod(t.valueType, genID(sid) + "_value_id")
      }
      case _ =>
    }
    dictionary
  }

  override def fieldsToDict(
    fields: Seq[Field],
    blacklist: Seq[String],
    namespace: Option[Identifier] = None
  ) = {
    val dictionaries = super.fieldsToDict(fields, blacklist)

    (dictionaries, fields, 0 until dictionaries.size).zipped.foreach {
      case (dictionary, field, index) =>
        dictionary("decodeMethod") = getFieldNSCoderMethod(field, true)
        dictionary("encodeMethod") = getFieldNSCoderMethod(field, false)
        dictionary("fieldNameCamelCase") = genID(field.sid.toCamelCase).toString
        dictionary("fieldNameInInit") =
          genID(if (index == 0) field.sid.toTitleCase else field.sid.toCamelCase).toString
        dictionary("isPrimitive") = isPrimitive(field.fieldType) || field.fieldType
          .isInstanceOf[EnumType]
        dictionary("wireConstType") = genWireConstType(field.fieldType).data
    }

    dictionaries
  }

  override def structDict(
    struct: StructLike,
    namespace: Option[Identifier],
    includes: Seq[Include],
    serviceOptions: Set[ServiceOption],
    genAdapt: Boolean,
    toplevel: Boolean = false
  ) = {
    val dictionary = super.structDict(struct, namespace, includes, serviceOptions, genAdapt)
    dictionary("headers") = getDependentHeaders(struct)

    dictionary
  }

  private[this] object CocoaKeywords {
    private[this] val set = Set[String](
      "auto",
      "break",
      "case",
      "char",
      "const",
      "continue",
      "default",
      "do",
      "double",
      "else",
      "enum",
      "extern",
      "float",
      "for",
      "goto",
      "if",
      "inline",
      "int",
      "long",
      "register",
      "restrict",
      "return",
      "short",
      "signed",
      "sizeof",
      "static",
      "struct",
      "switch",
      "typedef",
      "union",
      "unsigned",
      "void",
      "volatile",
      "while",
      "_bool",
      "_complex",
      "_imaginary",
      "bool",
      "class",
      "bycopy",
      "byref",
      "id",
      "imp",
      "in",
      "inout",
      "nil",
      "no",
      "null",
      "oneway",
      "out",
      "protocol",
      "sel",
      "self",
      "super",
      "yes",
      "atomic",
      "nonatomic",
      "retain",
      "strong"
    )
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
    doc.namespace(namespaceLanguage) getOrElse SimpleID(defaultNamespace)

  override def getIncludeNamespace(includeFileName: String): Identifier = {
    val cocoaNamespace = includeMap.get(includeFileName).flatMap { doc: ResolvedDocument =>
      doc.document.namespace(namespaceLanguage)
    }
    cocoaNamespace getOrElse SimpleID(defaultNamespace)
  }

  override def isLazyReadEnabled(t: FunctionType, optional: Boolean): Boolean = false

  override def qualifyNamedType(t: NamedType, namespace: Option[Identifier] = None): Identifier =
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
  def genStruct(struct: StructRHS, fieldType: Option[FieldType] = None): CodeFragment =
    throw new Exception("not implemented")
  def genUnion(struct: UnionRHS, fieldType: Option[FieldType] = None): CodeFragment =
    throw new Exception("not implemented")

  // For mutability/immutability support, not implemented
  def genToImmutable(t: FieldType): CodeFragment = v("")
  def genToImmutable(f: Field): CodeFragment = v("")
  def toMutable(t: FieldType): (String, String) = ("", "")
  def toMutable(f: Field): (String, String) = ("", "")

  def genType(t: FunctionType, immutable: Boolean = false): CodeFragment = {
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
  def getParentFinagleService(p: ServiceParent): CodeFragment =
    throw new Exception("not implemented")
  def getParentFinagleClient(p: ServiceParent): CodeFragment =
    throw new Exception("not implemented")

  private[this] def writeFile(file: File, fileHeader: String, fileContent: String): Unit = {
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
    serviceOptions: Set[ServiceOption],
    outputPath: File,
    dryRun: Boolean = false,
    genAdapt: Boolean = false
  ): Iterable[File] = {
    val generatedFiles = new mutable.ListBuffer[File]
    val doc = normalizeCase(resolvedDoc.document)
    val namespace = getNamespace(resolvedDoc.document)
    currentNamespace = namespace.fullName
    val packageDir = outputPath
    val includes = doc.headers.collect {
      case x: Include => x
    }

    val enumsWithNamespace = doc.enums.map(
      enum =>
        Enum(
          SimpleID(currentNamespace + enum.sid.name),
          enum.values,
          enum.docstring,
          enum.annotations
      )
    )

    if (!dryRun) {
      enumsWithNamespace.foreach { enum =>
        val hFile = new File(packageDir, enum.sid.toTitleCase.name + headerExtension)
        val dict = enumDict(namespace, enum)
        writeFile(hFile, headerTemplateLoader.header, headerTemplateLoader("enum").generate(dict))
      }
    }

    val structsWithNamespace = doc.structs.map {
      case union: Union =>
        Union(
          SimpleID(currentNamespace + union.sid.name),
          union.originalName,
          union.fields,
          union.docstring,
          union.annotations
        )
      case struct =>
        Struct(
          SimpleID(currentNamespace + struct.sid.name),
          struct.originalName,
          struct.fields,
          struct.docstring,
          struct.annotations
        )
    }

    if (!dryRun) {
      structsWithNamespace.foreach { struct =>
        val hFile = new File(packageDir, struct.sid.toTitleCase.name + headerExtension)
        val mFile = new File(packageDir, struct.sid.toTitleCase.name + fileExtension)

        val templateName = "struct"
        val dict = structDict(struct, Some(namespace), includes, serviceOptions, true)
        writeFile(
          mFile,
          implementationTemplateLoader.header,
          implementationTemplateLoader(templateName).generate(dict)
        )
        writeFile(
          hFile,
          headerTemplateLoader.header,
          headerTemplateLoader(templateName).generate(dict)
        )

        generatedFiles += hFile
        generatedFiles += mFile
      }
    }

    generatedFiles
  }
}
