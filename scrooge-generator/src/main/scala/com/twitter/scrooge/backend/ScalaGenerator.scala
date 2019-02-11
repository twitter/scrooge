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

package com.twitter.scrooge.backend

import com.twitter.scrooge.ast._
import com.twitter.scrooge.frontend.{ScroogeInternalException, ResolvedDocument}
import com.twitter.scrooge.mustache.Dictionary._
import com.twitter.scrooge.mustache.HandlebarLoader
import java.io.File

object ScalaGeneratorFactory extends GeneratorFactory {
  val language = "scala"
  val handlebarLoader = new HandlebarLoader("/scalagen/", ".mustache")
  def apply(
    doc: ResolvedDocument,
    defaultNamespace: String,
    experimentFlags: Seq[String]
  ): Generator = new ScalaGenerator(doc, defaultNamespace, experimentFlags, handlebarLoader)
}

class ScalaGenerator(
  override val resolvedDoc: ResolvedDocument,
  val defaultNamespace: String,
  val experimentFlags: Seq[String],
  val templatesLoader: HandlebarLoader)
    extends TemplateGenerator(resolvedDoc) {
  def templates: HandlebarLoader = templatesLoader
  val namespaceLanguage = "scala"

  val fileExtension = ".scala"

  var warnOnJavaNamespaceFallback: Boolean = false

  private object ScalaKeywords {
    private[this] val set = Set[String](
      "abstract",
      "case",
      "catch",
      "class",
      "def",
      "do",
      "else",
      "extends",
      "false",
      "final",
      "finally",
      "for",
      "forSome",
      "if",
      "implicit",
      "import",
      "lazy",
      "macro",
      "match",
      "new",
      "null",
      "object",
      "override",
      "package",
      "private",
      "protected",
      "return",
      "sealed",
      "super",
      "this",
      "throw",
      "trait",
      "try",
      "true",
      "type",
      "val",
      "var",
      "while",
      "with",
      "yield"
    )
    def contains(str: String): Boolean = set.contains(str)
  }

  // Quote Scala reserved words in ``
  def quoteKeyword(str: String): String =
    if (ScalaKeywords.contains(str))
      "`" + str + "`"
    else
      str

  private[this] def getNamespaceWithWarning(doc: Document): Option[Identifier] =
    doc.namespace("scala") orElse {
      val ns = doc.namespace("java")
      if (ns.isDefined && warnOnJavaNamespaceFallback)
        println("falling back to the java namespace. this will soon be deprecated")
      ns
    }

  override protected def getIncludeNamespace(includeFileName: String): Identifier = {
    val javaNamespace = includeMap.get(includeFileName).flatMap { doc: ResolvedDocument =>
      getNamespaceWithWarning(doc.document)
    }
    javaNamespace.getOrElse(SimpleID(defaultNamespace))
  }

  override def getNamespace(doc: Document): Identifier =
    getNamespaceWithWarning(doc).getOrElse(SimpleID(defaultNamespace))

  def genList(list: ListRHS, fieldType: Option[FieldType] = None): CodeFragment = {
    val listElemType = fieldType.map(_.asInstanceOf[ListType].eltType)
    val code =
      list.elems
        .map { e =>
          genConstant(e, listElemType).toData
        }
        .mkString(", ")
    v(s"Seq($code)")
  }

  def genSet(set: SetRHS, fieldType: Option[FieldType] = None): CodeFragment = {
    val setElemType = fieldType.map(_.asInstanceOf[SetType].eltType)
    val code = set.elems
      .map { e =>
        genConstant(e, setElemType).toData
      }
      .mkString(", ")
    v(s"Set($code)")
  }

  def genMap(map: MapRHS, fieldType: Option[FieldType] = None): CodeFragment = {
    val mapType = fieldType.map(_.asInstanceOf[MapType])
    val code = map.elems
      .map {
        case (k, v) =>
          val key = genConstant(k, mapType.map(_.keyType)).toData
          val value = genConstant(v, mapType.map(_.valueType)).toData
          s"$key -> $value"
      }
      .mkString(", ")

    v(s"Map($code)")
  }

  def genEnum(enum: EnumRHS, fieldType: Option[FieldType] = None): CodeFragment = {
    def getTypeId: Identifier = fieldType.getOrElse(Void) match {
      case n: NamedType => qualifyNamedType(n)
      case _ => enum.enum.sid
    }
    genID(enum.value.sid.toTitleCase.addScope(getTypeId.toTitleCase))
  }

  def genStruct(struct: StructRHS, fieldType: Option[FieldType] = None): CodeFragment = {
    val values = struct.elems
    val fields = values.map {
      case (f, value) =>
        val v = genConstant(value, Some(f.fieldType))
        genID(f.sid.toCamelCase) + " = " + (if (f.requiredness.isOptional) "Some(" + v + ")" else v)
    }

    val gid = fieldType match {
      case Some(t) => genType(t)
      case None => genID(struct.sid)
    }

    v(gid + "(" + fields.mkString(", ") + ")")
  }

  def genUnion(union: UnionRHS, fieldType: Option[FieldType] = None): CodeFragment = {
    val fieldId = genID(union.field.sid.toTitleCase)
    val unionId = fieldType match {
      case Some(t) => genType(t)
      case None => genID(union.sid)
    }
    val rhs = genConstant(union.initializer, Some(union.field.fieldType))
    v(s"$unionId.$fieldId($rhs)")
  }

  override def genDefaultValue(fieldType: FieldType): CodeFragment = {
    val code = fieldType match {
      case TI64 => "0L"
      case MapType(_, _, _) | SetType(_, _) | ListType(_, _) =>
        genType(fieldType).toData + "()"
      case _ => super.genDefaultValue(fieldType).toData
    }
    v(code)
  }

  override def genConstant(constant: RHS, fieldType: Option[FieldType] = None): CodeFragment = {
    (constant, fieldType) match {
      case (IntLiteral(value), Some(TI64)) => v(value.toString + "L")
      case _ => super.genConstant(constant, fieldType)
    }
  }

  def genType(t: FunctionType, immutable: Boolean = false): CodeFragment = {
    val prefix = if (immutable) "_root_.scala.collection.immutable." else "_root_.scala.collection."
    val code = t match {
      case Void => "Unit"
      case OnewayVoid => "Unit"
      case TBool => "Boolean"
      case TByte => "Byte"
      case TI16 => "Short"
      case TI32 => "Int"
      case TI64 => "Long"
      case TDouble => "Double"
      case TString => "String"
      case TBinary => "_root_.java.nio.ByteBuffer"
      case MapType(k, v, _) =>
        prefix + "Map[" + genType(k, immutable).toData + ", " + genType(v, immutable).toData + "]"
      case SetType(x, _) =>
        prefix + "Set[" + genType(x, immutable).toData + "]"
      case ListType(x, _) =>
        // for historical reasons
        "_root_.scala.collection.Seq[" + genType(x, immutable).toData + "]"
      case t: NamedType =>
        val id = resolvedDoc.qualifyName(t, namespaceLanguage, defaultNamespace)
        // Named types are capitalized.
        genID(id.toTitleCase).toData
      case r: ReferenceType =>
        throw new ScroogeInternalException("ReferenceType should not appear in backend")
    }
    v(code)
  }

  def genPrimitiveType(t: FunctionType): CodeFragment = genType(t)

  def genFieldType(f: Field): CodeFragment = {
    val baseType = genType(f.fieldType).toData
    val code =
      if (f.requiredness.isOptional) {
        "Option[" + baseType + "]"
      } else {
        baseType
      }
    v(code)
  }

  def genFieldParams(fields: Seq[Field], asVal: Boolean = false): CodeFragment = {
    val code = fields
      .map { f =>
        val valPrefix = if (asVal) "val " else ""
        val nameAndType = genID(f.sid).toData + ": " + genFieldType(f).toData
        val defaultValue =
          genDefaultFieldValue(f)
            .map { d =>
              " = " + d.toData
            }
            .getOrElse {
              if (f.requiredness.isOptional) " = None"
              else ""
            }

        valPrefix + nameAndType + defaultValue
      }
      .mkString(", ")
    v(code)
  }

  def genBaseFinagleService: CodeFragment =
    v("com.twitter.finagle.Service[Array[Byte], Array[Byte]]")

  def getParentFinagleService(p: ServiceParent): CodeFragment =
    genID(Identifier(getServiceParentID(p).fullName + "$FinagleService"))

  def getParentFinagleClient(p: ServiceParent): CodeFragment =
    genID(Identifier(getServiceParentID(p).fullName + "$FinagleClient"))

  override def finagleClientFile(
    packageDir: File,
    service: Service,
    options: Set[ServiceOption]
  ): Option[File] =
    options.find(_ == WithFinagle) map { _ =>
      new File(packageDir, service.sid.toTitleCase.name + "$FinagleClient" + fileExtension)
    }

  override def finagleServiceFile(
    packageDir: File,
    service: Service,
    options: Set[ServiceOption]
  ): Option[File] =
    options.find(_ == WithFinagle) map { _ =>
      new File(packageDir, service.sid.toTitleCase.name + "$FinagleService" + fileExtension)
    }
}
