package com.twitter.scrooge.backend

/*
 * Copyright 2011 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.twitter.scrooge.android_generator.AndroidGeneratorFactory
import com.twitter.scrooge.ast._
import com.twitter.scrooge.backend.lua.LuaGeneratorFactory
import com.twitter.scrooge.frontend.{ResolvedDocument, ScroogeInternalException}
import com.twitter.scrooge.java_generator.ApacheJavaGeneratorFactory
import com.twitter.scrooge.mustache.Dictionary.{NoValue, v}
import com.twitter.scrooge.mustache.{Dictionary, HandlebarLoader}
import java.io.{File, FileOutputStream, OutputStreamWriter}
import scala.collection.JavaConverters._
import scala.collection.mutable

abstract sealed class ServiceOption

case object WithFinagle extends ServiceOption
case object WithAsClosable extends ServiceOption {
  val AsClosableMethodName: String = "asClosable"
}
case class JavaService(service: Service, options: Set[ServiceOption])

object Generator {

  /**
   * Annotation used for fields which are required for new instances of a struct, but which are
   * optional for the purpose of reading and serialization.
   */
  val ConstructionRequiredAnnotation = "construction_required"

  def isConstructionRequiredField(field: Field): Boolean = {
    val hasAnnotation =
      field.fieldAnnotations.getOrElse(ConstructionRequiredAnnotation, "false").toBoolean
    if (hasAnnotation) {
      if (field.requiredness != Requiredness.Optional) {
        throw new ConstructionRequiredAnnotationException(
          field = field.sid.name
        )
      }
      true
    } else {
      false
    }
  }
}

abstract class Generator(doc: ResolvedDocument) {

  /**
   * @param genAdapt Generate code for Adaptive Decoding.
   *                 This flag is only used for scala presently.
   */
  def apply(
    serviceOptions: Set[ServiceOption],
    outputPath: File,
    dryRun: Boolean = false,
    genAdapt: Boolean = false
  ): Iterable[File]

  /**
   * Used to find the namespace in included files.
   * This does not need to match the corresponding GeneratorFactory.language.
   */
  def namespaceLanguage: String

  def includeMap: Map[String, ResolvedDocument] = doc.resolver.includeMap
}

object GeneratorFactory {
  private[this] val factories: Map[String, GeneratorFactory] = {
    val klass = classOf[GeneratorFactory]
    val loadedGenerators =
      java.util.ServiceLoader.load(klass, klass.getClassLoader).iterator.asScala
    val factories =
      List(
        ScalaGeneratorFactory,
        ApacheJavaGeneratorFactory,
        AndroidGeneratorFactory,
        CocoaGeneratorFactory,
        LuaGeneratorFactory
      ) ++
        loadedGenerators

    factories.map { g =>
      (g.language -> g)
    }.toMap
  }

  def languages = factories.keys

  def apply(
    lan: String,
    doc: ResolvedDocument,
    defaultNamespace: String,
    experimentFlags: Seq[String]
  ): Generator = factories.get(lan) match {
    case Some(factory) => factory(doc, defaultNamespace, experimentFlags)
    case None => throw new Exception("Generator for language \"%s\" not found".format(lan))
  }
}

trait GeneratorFactory {

  /**
   * Command line language matches on this.
   */
  def language: String
  def apply(
    doc: ResolvedDocument,
    defaultNamespace: String,
    experimentFlags: Seq[String]
  ): Generator
}

object TemplateGenerator {

  /**
   * Renders a map as:
   *   Dictionary("pairs" -> ListValue(Seq(Dictionary("key" -> ..., "value" -> ...)))
   */
  def renderPairs(pairs: Map[String, String]): Dictionary.Value = {
    if (pairs.isEmpty) {
      NoValue
    } else {
      val pairDicts: Seq[Dictionary] =
        pairs.map { case (key, value) => Dictionary("key" -> v(key), "value" -> v(value)) }.toSeq
      v(Dictionary("pairs" -> v(pairDicts)))
    }
  }

  def namespacedFolder(destFolder: File, namespace: String, dryRun: Boolean): File = {
    val file = new File(destFolder, namespace.replace('.', File.separatorChar))
    if (!dryRun) file.mkdirs()
    file
  }

  def getNamespace(doc: Document, namespaceLanguage: String, defaultNamespace: String): Identifier =
    doc.namespace(namespaceLanguage).getOrElse(SimpleID(defaultNamespace))

  def normalizeCase[N <: Node](node: N): N = {
    (node match {
      case d: Document =>
        d.copy(defs = d.defs.map(normalizeCase))
      case id: Identifier => id.toTitleCase
      case e: EnumRHS =>
        e.copy(normalizeCase(e.enum), normalizeCase(e.value))
      case f: Field =>
        f.copy(sid = f.sid.toCamelCase, default = f.default.map(normalizeCase))
      case f: Function =>
        f.copy(
          funcName = f.funcName.toCamelCase,
          args = f.args.map(normalizeCase),
          throws = f.throws.map(normalizeCase)
        )
      case c: ConstDefinition =>
        c.copy(value = normalizeCase(c.value))
      case e: Enum =>
        e.copy(values = e.values.map(normalizeCase))
      case e: EnumField =>
        e.copy(sid = e.sid.toTitleCase)
      case s: Struct =>
        s.copy(fields = s.fields.map(normalizeCase))
      case f: FunctionArgs =>
        f.copy(fields = f.fields.map(normalizeCase))
      case f: FunctionResult =>
        f.copy(success = f.success.map(normalizeCase), exceptions = f.exceptions.map(normalizeCase))
      case e: Exception_ =>
        e.copy(fields = e.fields.map(normalizeCase))
      case s: Service =>
        s.copy(functions = s.functions.map(normalizeCase))
      case n => n
    }).asInstanceOf[N]
  }
}

abstract class TemplateGenerator(val resolvedDoc: ResolvedDocument)
    extends Generator(resolvedDoc)
    with StructTemplate
    with ServiceTemplate
    with ConstsTemplate
    with EnumTemplate {
  import Dictionary._
  import Generator._

  /**
   * Map from included file names to the namespaces defined in those files.
   */
  val defaultNamespace: String
  val experimentFlags: Seq[String]

  /******************** helper functions ************************/
  protected def namespacedFolder(destFolder: File, namespace: String, dryRun: Boolean): File =
    TemplateGenerator.namespacedFolder(destFolder, namespace, dryRun)

  protected def getIncludeNamespace(includeFileName: String): Identifier =
    includeMap
      .get(includeFileName)
      .map { doc: ResolvedDocument =>
        getNamespace(doc.document)
      }
      .getOrElse(SimpleID(defaultNamespace))

  def getNamespace(doc: Document): Identifier =
    TemplateGenerator.getNamespace(doc, namespaceLanguage, defaultNamespace)

  def normalizeCase[N <: Node](node: N): N =
    TemplateGenerator.normalizeCase(node)

  def quote(str: String) = "\"" + str + "\""
  def quoteKeyword(str: String): String
  def isNullableType(t: FieldType, isOptional: Boolean = false) = {
    !isOptional && (
      t match {
        case TBool | TByte | TI16 | TI32 | TI64 | TDouble => false
        case _ => true
      }
    )
  }

  def getServiceParentID(parent: ServiceParent): Identifier = {
    val identifier: Identifier = parent.filename match {
      case Some(scope) => parent.sid.addScope(getIncludeNamespace(scope.name))
      case None => parent.sid
    }
    identifier.toTitleCase
  }

  def getParentFinagleService(parent: ServiceParent): CodeFragment
  def getParentFinagleClient(parent: ServiceParent): CodeFragment

  def isPrimitive(t: FunctionType): Boolean = {
    t match {
      case Void | TBool | TByte | TI16 | TI32 | TI64 | TDouble => true
      case _ => false
    }
  }

  def isLazyReadEnabled(t: FunctionType, optional: Boolean): Boolean = {
    t match {
      case TString => true
      case Void | TBool | TByte | TI16 | TI32 | TI64 | TDouble => optional
      case _ => false
    }
  }

  protected def writeFile(file: File, fileHeader: String, fileContent: String): Unit = {
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

  // methods that convert AST nodes to CodeFragment
  def genID(data: Identifier): CodeFragment = data match {
    case SimpleID(name, _) => v(quoteKeyword(name))
    case QualifiedID(names) => v(names.map(quoteKeyword).mkString("."))
  }

  // Add namespace if id is unqualified.
  def genQualifiedID(id: Identifier, namespace: Identifier): CodeFragment =
    id match {
      case sid: SimpleID => genID(sid.addScope(namespace))
      case qid: QualifiedID => genID(qid)
    }

  def genConstant(constant: RHS, fieldType: Option[FieldType] = None): CodeFragment = {
    constant match {
      case NullLiteral => v("null")
      case StringLiteral(value) => v(quote(value))
      case DoubleLiteral(value) => v(value.toString)
      case IntLiteral(value) => v(value.toString)
      case BoolLiteral(value) => v(value.toString)
      case c @ ListRHS(_) => genList(c, fieldType)
      case c @ SetRHS(_) => genSet(c, fieldType)
      case c @ MapRHS(_) => genMap(c, fieldType)
      case c: EnumRHS => genEnum(c, fieldType)
      case iv @ IdRHS(id) => genID(id)
      case s: StructRHS => genStruct(s, fieldType)
      case u: UnionRHS => genUnion(u, fieldType)
    }
  }

  def genList(list: ListRHS, fieldType: Option[FieldType] = None): CodeFragment

  def genSet(set: SetRHS, fieldType: Option[FieldType] = None): CodeFragment

  def genMap(map: MapRHS, fieldType: Option[FieldType] = None): CodeFragment

  def genEnum(enum: EnumRHS, fieldType: Option[FieldType] = None): CodeFragment

  def genStruct(struct: StructRHS, fieldType: Option[FieldType] = None): CodeFragment

  def genUnion(union: UnionRHS, fieldType: Option[FieldType] = None): CodeFragment

  /**
   * The default value for the specified type and mutability.
   */
  def genDefaultValue(fieldType: FieldType): CodeFragment = {
    val code = fieldType match {
      case TBool => "false"
      case TByte | TI16 | TI32 => "0"
      case TDouble => "0.0"
      case _ => "null"
    }
    v(code)
  }

  def genDefaultFieldValueForFieldInfo(f: Field): Option[CodeFragment] = {
    if (f.requiredness.isOptional) {
      None
    } else {
      f.default.map(genConstant(_, Some(f.fieldType)))
    }
  }

  /**
   * Creates a code fragment for the default value of the field.
   * @param f field to generate the default value for.
   * @param forAlternateConstructor Whether this is for the alternate Immutable constructor which
   *                                does not take an Option for construction required fields.
   */
  def genDefaultFieldValue(
    f: Field,
    forAlternateConstructor: Boolean = false
  ): Option[CodeFragment] = {
    if (f.requiredness.isOptional || (forAlternateConstructor && isConstructionRequiredField(f))) {
      None
    } else {
      f.default.map(genConstant(_, Some(f.fieldType))).orElse {
        if (f.fieldType.isInstanceOf[ContainerType]) {
          Some(genDefaultValue(f.fieldType))
        } else {
          None
        }
      }
    }
  }

  def genDefaultReadValue(f: Field): CodeFragment =
    genDefaultFieldValue(f).getOrElse(genDefaultValue(f.fieldType))

  def genConstType(t: FunctionType): CodeFragment = {
    val code = t match {
      case Void => "VOID"
      case TBool => "BOOL"
      case TByte => "BYTE"
      case TDouble => "DOUBLE"
      case TI16 => "I16"
      case TI32 => "I32"
      case TI64 => "I64"
      case TString => "STRING"
      case TBinary => "STRING" // thrift's idea of "string" is based on old broken c++ semantics.
      case StructType(_, _) => "STRUCT"
      case EnumType(_, _) => "ENUM"
      case MapType(_, _, _) => "MAP"
      case SetType(_, _) => "SET"
      case ListType(_, _) => "LIST"
      case x => throw new InternalError("constType#" + t)
    }
    v(code)
  }

  /**
   * When a named type is imported via include statement, we need to
   * qualify it using its full namespace
   */
  def qualifyNamedType(t: NamedType, namespace: Option[Identifier] = None): Identifier =
    t.scopePrefix match {
      case Some(scope) => t.sid.addScope(getIncludeNamespace(scope.name))
      case None if namespace.isDefined => t.sid.addScope(namespace.get)
      case None => t.sid
    }

  def genProtocolReadMethod(t: FunctionType): CodeFragment = {
    val code = t match {
      case TBool => "readBool"
      case TByte => "readByte"
      case TI16 => "readI16"
      case TI32 => "readI32"
      case TI64 => "readI64"
      case TDouble => "readDouble"
      case TString => "readString"
      case TBinary => "readBinary"
      case x => throw new ScroogeInternalException("genProtocolReadMethod#" + t)
    }
    v(code)
  }

  def genProtocolSkipMethod(t: FunctionType): CodeFragment = {
    val code = t match {
      case TBool => "offsetSkipBool"
      case TByte => "offsetSkipBool"
      case TI16 => "offsetSkipI16"
      case TI32 => "offsetSkipI32"
      case TI64 => "offsetSkipI64"
      case TDouble => "offsetSkipDouble"
      case TString => "offsetSkipString"
      case TBinary => "offsetSkipBinary"
      case x => throw new ScroogeInternalException("genProtocolSkipMethod#" + t)
    }
    v(code)
  }

  def genOffsetSkipProtocolMethod(t: FunctionType): CodeFragment = {
    val code = t match {
      case TBool => "offsetSkipBool"
      case TByte => "offsetSkipByte"
      case TI16 => "offsetSkipI16"
      case TI32 => "offsetSkipI32"
      case TI64 => "offsetSkipI64"
      case TDouble => "offsetSkipDouble"
      case TString => "offsetSkipString"
      case TBinary => "offsetSkipBinary"
      case x =>
        s"""Invalid type passed($x) for genOffsetSkipProtocolMethod method. Compile will fail here."""
    }
    v(code)
  }

  def genDecodeProtocolMethod(t: FunctionType): CodeFragment = {
    val code = t match {
      case TBool => "decodeBool"
      case TByte => "decodeByte"
      case TI16 => "decodeI16"
      case TI32 => "decodeI32"
      case TI64 => "decodeI64"
      case TDouble => "decodeDouble"
      case TString => "decodeString"
      case TBinary => "decodeBinary"
      case x =>
        s"""Invalid type passed ($x) for genDecodeProtocolMethod method. Compile will fail here."""
    }
    v(code)
  }

  def genProtocolWriteMethod(t: FunctionType): CodeFragment = {
    val code = t match {
      case TBool => "writeBool"
      case TByte => "writeByte"
      case TI16 => "writeI16"
      case TI32 => "writeI32"
      case TI64 => "writeI64"
      case TDouble => "writeDouble"
      case TString => "writeString"
      case TBinary => "writeBinary"
      case x => throw new ScroogeInternalException("protocolWriteMethod#" + t)
    }
    v(code)
  }

  def genType(t: FunctionType, immutable: Boolean = false): CodeFragment

  def genPrimitiveType(t: FunctionType): CodeFragment

  def genFieldType(f: Field): CodeFragment

  def genFieldParams(fields: Seq[Field], asVal: Boolean = false): CodeFragment

  def genBaseFinagleService: CodeFragment

  def finagleClientFile(
    packageDir: File,
    service: Service,
    options: Set[ServiceOption]
  ): Option[File] =
    None

  def finagleServiceFile(
    packageDir: File,
    service: Service,
    options: Set[ServiceOption]
  ): Option[File] =
    None

  def templates: HandlebarLoader
  def fileExtension: String

  def apply(
    serviceOptions: Set[ServiceOption],
    outputPath: File,
    dryRun: Boolean = false,
    genAdapt: Boolean = false
  ): Iterable[File] = {
    val generatedFiles = new mutable.ListBuffer[File]
    val doc = normalizeCase(resolvedDoc.document)
    val namespace = getNamespace(resolvedDoc.document)
    val packageDir = namespacedFolder(outputPath, namespace.fullName, dryRun)
    val includes = doc.headers.collect {
      case x @ Include(_, _) => x
    }

    if (doc.consts.nonEmpty) {
      val file = new File(packageDir, "Constants" + fileExtension)
      if (!dryRun) {
        val dict = constDict(namespace, doc.consts)
        writeFile(file, templates.header, templates("consts").generate(dict))
      }
      generatedFiles += file
    }

    doc.enums.foreach { enum =>
      val file = new File(packageDir, enum.sid.toTitleCase.name + fileExtension)
      if (!dryRun) {
        val dict = enumDict(namespace, enum)
        writeFile(file, templates.header, templates("enum").generate(dict))
      }
      generatedFiles += file
    }

    doc.structs.foreach { struct =>
      val file = new File(packageDir, struct.sid.toTitleCase.name + fileExtension)

      if (!dryRun) {
        val templateName =
          struct match {
            case _: Union => "union"
            case _ => "struct"
          }

        val dict = structDict(struct, Some(namespace), includes, serviceOptions, genAdapt, true)
        writeFile(file, templates.header, templates(templateName).generate(dict))
      }
      generatedFiles += file
    }

    doc.services.foreach { service =>
      val allOptions = serviceOptions ++ service.options
      val interfaceFile = new File(packageDir, service.sid.toTitleCase.name + fileExtension)
      val finagleClientFileOpt = finagleClientFile(packageDir, service, allOptions)
      val finagleServiceFileOpt = finagleServiceFile(packageDir, service, allOptions)

      if (!dryRun) {
        val dict = serviceDict(service, namespace, includes, allOptions, genAdapt)
        writeFile(interfaceFile, templates.header, templates("service").generate(dict))

        finagleClientFileOpt foreach { file =>
          val dict = finagleClient(service, namespace, allOptions.contains(WithAsClosable))
          writeFile(file, templates.header, templates("finagleClient").generate(dict))
        }

        finagleServiceFileOpt foreach { file =>
          val dict = finagleService(service, namespace)
          writeFile(file, templates.header, templates("finagleService").generate(dict))
        }
      }
      generatedFiles += interfaceFile
      generatedFiles ++= finagleServiceFileOpt
      generatedFiles ++= finagleClientFileOpt
    }

    generatedFiles
  }

  /**
   * Returns a String "_root_.scala.Product${N}[Type1, Type2, ...]" or "scala.Product".
   */
  def productN(fields: Seq[Field], namespace: Option[Identifier]): String = {
    val arity = fields.length
    if (arity >= 1 && arity <= 22) {
      val fieldTypes = fields
        .map { f =>
          genFieldType(f).toData
        }
        .mkString(", ")
      s"_root_.scala.Product$arity[$fieldTypes]"
    } else {
      "_root_.scala.Product"
    }
  }

  /**
   * Returns a String "_root_.scala.Tuple${N}[Type1, Type2, ...]"
   */
  def tupleN(fields: Seq[Field], namespace: Option[Identifier]): String = {
    val arity = fields.length
    if (arity >= 1 && arity <= 22) {
      val fieldTypes = fields
        .map { f =>
          genFieldType(f).toData
        }
        .mkString(", ")
      s"_root_.scala.Tuple$arity[$fieldTypes]"
    } else {
      "_root_.scala.Product"
    }
  }
}
