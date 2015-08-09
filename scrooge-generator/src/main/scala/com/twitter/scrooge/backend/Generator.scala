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

import com.twitter.finagle.util.LoadService
import com.twitter.scrooge.ast._
import com.twitter.scrooge.frontend.{ResolvedDocument, ScroogeInternalException}
import com.twitter.scrooge.java_generator.ApacheJavaGeneratorFactory
import com.twitter.scrooge.mustache.Dictionary.CodeFragment
import com.twitter.scrooge.android_generator.AndroidGeneratorFactory
import com.twitter.scrooge.mustache.{Dictionary, HandlebarLoader}
import java.io.{File, FileOutputStream, OutputStreamWriter}
import scala.collection.mutable

abstract sealed class ServiceOption

case object WithFinagle extends ServiceOption
case class JavaService(service: Service, options: Set[ServiceOption])

trait ThriftGenerator {
  def apply(
    _doc: Document,
    serviceOptions: Set[ServiceOption],
    outputPath: File,
    dryRun: Boolean = false): Iterable[File]
}

object GeneratorFactory {
  private[this] val factories: Map[String, GeneratorFactory] = {
    val loadedGenerators = LoadService[GeneratorFactory]()
    val factories =
      List(JavaGeneratorFactory, ScalaGeneratorFactory, ApacheJavaGeneratorFactory, 
        AndroidGeneratorFactory, CocoaGeneratorFactory) ++
      loadedGenerators

    (factories map { g => (g.lang -> g) }).toMap
  }

  def languages = factories.keys

  def apply(
    lan: String,
    includeMap: Map[String, ResolvedDocument],
    defaultNamespace: String,
    experimentFlags: Seq[String]
  ): ThriftGenerator = factories.get(lan) match {
    case Some(factory) => factory(includeMap, defaultNamespace, experimentFlags)
    case None => throw new Exception("Generator for language \"%s\" not found".format(lan))
  }
}

trait GeneratorFactory {
  def lang: String
  def apply(
    includeMap: Map[String, ResolvedDocument],
    defaultNamespace: String,
    experimentFlags: Seq[String]
  ): ThriftGenerator
}

trait TemplateGenerator
  extends ThriftGenerator
  with StructTemplate
  with ServiceTemplate
  with ConstsTemplate
  with EnumTemplate {
  import Dictionary._

  /**
   * Map from included file names to the namespaces defined in those files.
   */
  val includeMap: Map[String, ResolvedDocument]
  val defaultNamespace: String
  val experimentFlags: Seq[String]

  /******************** helper functions ************************/
  protected def namespacedFolder(destFolder: File, namespace: String, dryRun: Boolean): File = {
    val file = new File(destFolder, namespace.replace('.', File.separatorChar))
    if (!dryRun) file.mkdirs()
    file
  }

  protected def getIncludeNamespace(includeFileName: String): Identifier = {
    val javaNamespace = includeMap.get(includeFileName).flatMap {
      doc: ResolvedDocument => doc.document.namespace("java")
    }
    javaNamespace.getOrElse(SimpleID(defaultNamespace))
  }

  def normalizeCase[N <: Node](node: N): N = {
    (node match {
      case d: Document =>
        d.copy(defs = d.defs.map(normalizeCase))
      case id: Identifier => id.toTitleCase
      case e: EnumRHS =>
        e.copy(normalizeCase(e.enum), normalizeCase(e.value))
      case f: Field =>
        f.copy(
          sid = f.sid.toCamelCase,
          default = f.default.map(normalizeCase))
      case f: Function =>
        f.copy(
          funcName = f.funcName.toCamelCase,
          args = f.args.map(normalizeCase),
          throws = f.throws.map(normalizeCase))
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

  def getNamespace(doc: Document): Identifier =
    doc.namespace("java") getOrElse (SimpleID(defaultNamespace))

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

  /**
   * get the ID of a service parent.  Java and Scala implementations are different.
   */
  def getServiceParentID(parent: ServiceParent): Identifier = {
    val identifier: Identifier = parent.prefix match {
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
      case c@ListRHS(_) => genList(c, fieldType)
      case c@SetRHS(_) => genSet(c, fieldType)
      case c@MapRHS(_) => genMap(c, fieldType)
      case c: EnumRHS => genEnum(c, fieldType)
      case iv@IdRHS(id) => genID(id)
      case s: StructRHS => genStruct(s)
      case u: UnionRHS => genUnion(u)
    }
  }

  def genList(list: ListRHS, fieldType: Option[FieldType] = None): CodeFragment

  def genSet(set: SetRHS, fieldType: Option[FieldType] = None): CodeFragment

  def genMap(map: MapRHS, fieldType: Option[FieldType] = None): CodeFragment

  def genEnum(enum: EnumRHS, fieldType: Option[FieldType] = None): CodeFragment

  def genStruct(struct: StructRHS): CodeFragment

  def genUnion(union: UnionRHS): CodeFragment

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

  def genDefaultFieldValue(f: Field): Option[CodeFragment] = {
    if (f.requiredness.isOptional) {
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
      case x => throw new ScroogeInternalException("protocolReadMethod#" + t)
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
      case x => s"""Invalid type passed($x) for genOffsetSkipProtocolMethod method. Compile will fail here."""
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
      case x => s"""Invalid type passed ($x) for genDecodeProtocolMethod method. Compile will fail here."""
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

  def genType(t: FunctionType, namespace: Option[Identifier] = None): CodeFragment

  def genPrimitiveType(t: FunctionType): CodeFragment

  def genFieldType(f: Field): CodeFragment

  def genFieldParams(fields: Seq[Field], asVal: Boolean = false): CodeFragment

  def genBaseFinagleService: CodeFragment

  def finagleClientFile(
    packageDir: File,
    service: Service, options:
    Set[ServiceOption]
  ): Option[File] =
    None

  def finagleServiceFile(
    packageDir: File,
    service: Service, options:
    Set[ServiceOption]
  ): Option[File] =
    None


  def templates: HandlebarLoader
  def fileExtension: String

  def apply(
    _doc: Document,
    serviceOptions: Set[ServiceOption],
    outputPath: File,
    dryRun: Boolean = false
  ): Iterable[File] = {
    val generatedFiles = new mutable.ListBuffer[File]
    val doc = normalizeCase(_doc)
    val namespace = getNamespace(_doc)
    val packageDir = namespacedFolder(outputPath, namespace.fullName, dryRun)
    val includes = doc.headers.collect {
      case x@Include(_, _) => x
    }

    if (doc.consts.nonEmpty) {
      val file = new File(packageDir, "Constants" + fileExtension)
      if (!dryRun) {
        val dict = constDict(namespace, doc.consts)
        writeFile(file, templates.header, templates("consts").generate(dict))
      }
      generatedFiles += file
    }

    doc.enums.foreach {
      enum =>
        val file = new File(packageDir, enum.sid.toTitleCase.name + fileExtension)
        if (!dryRun) {
          val dict = enumDict(namespace, enum)
          writeFile(file, templates.header, templates("enum").generate(dict))
        }
        generatedFiles += file
    }

    doc.structs.foreach {
      struct =>
        val file = new File(packageDir, struct.sid.toTitleCase.name + fileExtension)

        if (!dryRun) {
          val templateName =
            struct match {
              case _: Union => "union"
              case _ => "struct"
            }

          val dict = structDict(struct, Some(namespace), includes, serviceOptions, true)
          writeFile(file, templates.header, templates(templateName).generate(dict))
        }
        generatedFiles += file
    }

    doc.services.foreach {
      service =>
        val interfaceFile = new File(packageDir, service.sid.toTitleCase.name + fileExtension)
        val finagleClientFileOpt = finagleClientFile(packageDir, service, serviceOptions)
        val finagleServiceFileOpt = finagleServiceFile(packageDir, service, serviceOptions)

        if (!dryRun) {
          val dict = serviceDict(service, namespace, includes, serviceOptions)
          writeFile(interfaceFile, templates.header, templates("service").generate(dict))

          finagleClientFileOpt foreach { file =>
            val dict = finagleClient(service, namespace)
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
   * Returns a String "scala.Product${N}[Type1, Type2, ...]" or "scala.Product".
   */
  def productN(fields: Seq[Field]): String = {
    val arity = fields.length
    if (arity >= 1 && arity <= 22) {
      val fieldTypes = fields.map { f =>
        genFieldType(f).toData
      }.mkString(", ")
      s"scala.Product$arity[$fieldTypes]"
    } else {
      "scala.Product"
    }
  }

  /**
   * Like productN, but returns "Unit" for empty lists.
   */
  def productNOrUnit(fields: Seq[Field]): String = {
    if (fields.isEmpty) "Unit"
    else productN(fields)
  }
}
