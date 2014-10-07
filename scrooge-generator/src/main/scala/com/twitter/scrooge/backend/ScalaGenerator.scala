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
  val lang = "scala"
  val handlebarLoader = new HandlebarLoader("/scalagen/", ".scala")
  def apply(
    includeMap: Map[String, ResolvedDocument],
    defaultNamespace: String,
    experimentFlags: Seq[String]
  ): ThriftGenerator = new ScalaGenerator(
    includeMap,
    defaultNamespace,
    experimentFlags,
    handlebarLoader)
}

class ScalaGenerator(
  val includeMap: Map[String, ResolvedDocument],
  val defaultNamespace: String,
  val experimentFlags: Seq[String],
  val templatesLoader: HandlebarLoader
) extends TemplateGenerator {
  def templates: HandlebarLoader = templatesLoader

  val fileExtension = ".scala"

  var warnOnJavaNamespaceFallback: Boolean = false

  private object ScalaKeywords {
    private[this] val set = Set[String](
      "abstract", "case", "catch", "class", "def", "do", "else", "extends",
      "false", "final", "finally", "for", "forSome", "if", "implicit", "import",
      "lazy", "match", "new", "null", "object", "override", "package", "private",
      "protected", "return", "sealed", "super", "this", "throw", "trait", "try",
      "true", "type", "val", "var", "while", "with", "yield")
    def contains(str: String): Boolean = set.contains(str)
  }

  // Quote Scala reserved words in ``
  def quoteKeyword(str: String): String =
    if (ScalaKeywords.contains(str))
      "`" + str + "`"
    else
      str

  def normalizeCase[N <: Node](node: N) = {
    (node match {
      case d: Document =>
        d.copy(defs = d.defs.map(normalizeCase(_)))
      case id: Identifier => id.toTitleCase
      case e: EnumRHS =>
        e.copy(normalizeCase(e.enum), normalizeCase(e.value))
      case f: Field =>
        f.copy(
          sid = f.sid.toCamelCase,
          default = f.default.map(normalizeCase(_)))
      case f: Function =>
        f.copy(
          args = f.args.map(normalizeCase(_)),
          throws = f.throws.map(normalizeCase(_)))
      case c: ConstDefinition =>
        c.copy(value = normalizeCase(c.value))
      case e: Enum =>
        e.copy(values = e.values.map(normalizeCase(_)))
      case e: EnumField =>
        e.copy(sid = e.sid.toTitleCase)
      case s: Struct =>
        s.copy(fields = s.fields.map(normalizeCase(_)))
      case f: FunctionArgs =>
        f.copy(fields = f.fields.map(normalizeCase(_)))
      case f: FunctionResult =>
        f.copy(fields = f.fields.map(normalizeCase(_)))
      case e: Exception_ =>
        e.copy(fields = e.fields.map(normalizeCase(_)))
      case s: Service =>
        s.copy(functions = s.functions.map(normalizeCase(_)))
      case n => n
    }).asInstanceOf[N]
  }

  private[this] def getNamespaceWithWarning(doc: Document): Option[Identifier] =
    doc.namespace("scala") orElse {
      val ns = doc.namespace("java")
      if (ns.isDefined && warnOnJavaNamespaceFallback)
        println("falling back to the java namespace. this will soon be deprecated")
      ns
    }

  override protected def getIncludeNamespace(includeFileName: String): Identifier = {
    val javaNamespace = includeMap.get(includeFileName).flatMap {
      doc: ResolvedDocument => getNamespaceWithWarning(doc.document)
    }
    javaNamespace.getOrElse(SimpleID(defaultNamespace))
  }

  override def getNamespace(doc: Document): Identifier =
    getNamespaceWithWarning(doc) getOrElse (SimpleID(defaultNamespace))

  def genList(list: ListRHS, mutable: Boolean = false): CodeFragment = {
    val code = (if (mutable) "mutable.Buffer(" else "Seq(") +
      list.elems.map(genConstant(_).toData).mkString(", ") + ")"
    codify(code)
  }

  def genSet(set: SetRHS, mutable: Boolean = false): CodeFragment = {
    val code = (if (mutable) "mutable.Set(" else "Set(") +
      set.elems.map(genConstant(_).toData).mkString(", ") + ")"
    codify(code)
  }

  def genMap(map: MapRHS, mutable: Boolean = false): CodeFragment = {
    val code = (if (mutable) "mutable.Map(" else "Map(") + (map.elems.map {
      case (k, v) =>
        genConstant(k).toData + " -> " + genConstant(v).toData
    } mkString (", ")) + ")"
    codify(code)
  }

  def genEnum(enum: EnumRHS, fieldType: Option[FieldType] = None): CodeFragment = {
    def getTypeId: Identifier = fieldType.getOrElse(Void) match {
      case n: NamedType => qualifyNamedType(n)
      case _ =>  enum.enum.sid
    }
    genID(enum.value.sid.toTitleCase.addScope(getTypeId.toTitleCase))
  }

  def genStruct(struct: StructRHS): CodeFragment = {
    val values = struct.elems
    val fields = values map { case (f, value) =>
      val v = genConstant(value)
      genID(f.sid.toCamelCase) + "=" + (if (f.requiredness.isOptional) "Some(" + v + ")" else v)
    }
    codify(genID(struct.sid) + "(" + fields.mkString(", ") + ")")
  }

  override def genDefaultValue(fieldType: FieldType, mutable: Boolean = false): CodeFragment = {
    val code = fieldType match {
      case TI64 => "0L"
      case MapType(_, _, _) | SetType(_, _) | ListType(_, _) =>
        genType(fieldType, mutable).toData + "()"
      case _ => super.genDefaultValue(fieldType, mutable).toData
    }
    codify(code)
  }

  override def genConstant(constant: RHS, mutable: Boolean = false, fieldType: Option[FieldType] = None): CodeFragment = {
    (constant, fieldType) match {
      case (IntLiteral(value), Some(TI64)) => codify(value.toString + "L")
      case _ => super.genConstant(constant, mutable, fieldType)
    }
  }

  /**
   * Generates a suffix to append to a field expression that will
   * convert the value to an immutable equivalent.
   */
  def genToImmutable(t: FieldType): CodeFragment = {
    val code = t match {
      case MapType(_, _, _) => ".toMap"
      case SetType(_, _) => ".toSet"
      case ListType(_, _) => ".toList"
      case _ => ""
    }
    codify(code)
  }

  /**
   * Generates a suffix to append to a field expression that will
   * convert the value to an immutable equivalent.
   */
  def genToImmutable(f: Field): CodeFragment = {
    if (f.requiredness.isOptional) {
      val code = genToImmutable(f.fieldType).toData match {
        case "" => ""
        case underlyingToImmutable => ".map(_" + underlyingToImmutable + ")"
      }
      codify(code)
    } else {
      genToImmutable(f.fieldType)
    }
  }

  /**
   * Generates a prefix and suffix to wrap around a field expression that will
   * convert the value to a mutable equivalent.
   */
  def toMutable(t: FieldType): (String, String) = {
    t match {
      case MapType(_, _, _) | SetType(_, _) => (genType(t, true).toData + "() ++= ", "")
      case ListType(_, _) => ("", ".toBuffer")
      case _ => ("", "")
    }
  }

  /**
   * Generates a prefix and suffix to wrap around a field expression that will
   * convert the value to a mutable equivalent.
   */
  def toMutable(f: Field): (String, String) = {
    if (f.requiredness.isOptional) {
      toMutable(f.fieldType) match {
        case ("", "") => ("", "")
        case (prefix, suffix) => ("", ".map(" + prefix + "_" + suffix + ")")
      }
    } else {
      toMutable(f.fieldType)
    }
  }

  def genType(t: FunctionType, mutable: Boolean = false): CodeFragment = {
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
      case TBinary => "ByteBuffer"
      case MapType(k, v, _) =>
        (if (mutable) "mutable." else "") + "Map[" + genType(k).toData + ", " + genType(v).toData + "]"
      case SetType(x, _) =>
        (if (mutable) "mutable." else "") + "Set[" + genType(x).toData + "]"
      case ListType(x, _) =>
        (if (mutable) "mutable.Buffer" else "Seq") + "[" + genType(x).toData + "]"
      case n: NamedType => genID(qualifyNamedType(n).toTitleCase).toData
      case r: ReferenceType =>
        throw new ScroogeInternalException("ReferenceType should not appear in backend")
    }
    codify(code)
  }

  def genPrimitiveType(t: FunctionType, mutable: Boolean = false): CodeFragment = genType(t, mutable)

  def genFieldType(f: Field, mutable: Boolean = false): CodeFragment = {
    val baseType = genType(f.fieldType, mutable).toData
    val code = if (f.requiredness.isOptional) {
      "Option[" + baseType + "]"
    } else {
      baseType
    }
    codify(code)
  }

  def genFieldParams(fields: Seq[Field], asVal: Boolean = false): CodeFragment = {
    val code = fields.map {
      f =>
        val valPrefix = if (asVal) "val " else ""
        val nameAndType = genID(f.sid).toData + ": " + genFieldType(f).toData
        val defaultValue = genDefaultFieldValue(f) map {
          " = " + _.toData
        }
        valPrefix + nameAndType + defaultValue.getOrElse("")
    }.mkString(", ")
    codify(code)
  }

  def genBaseFinagleService: CodeFragment = codify("FinagleService[Array[Byte], Array[Byte]]")

  def getParentFinagleService(p: ServiceParent): CodeFragment =
    genID(Identifier(getServiceParentID(p).fullName + "$FinagleService"))

  def getParentFinagleClient(p: ServiceParent): CodeFragment =
    genID(Identifier(getServiceParentID(p).fullName + "$FinagleClient"))

  override def finagleClientFile(
    packageDir: File,
    service: Service, options:
    Set[ServiceOption]
  ): Option[File] =
    options.find(_ == WithFinagle) map { _ =>
      new File(packageDir, service.sid.toTitleCase.name + "$FinagleClient" + fileExtension)
    }

  override def finagleServiceFile(
    packageDir: File,
    service: Service, options:
    Set[ServiceOption]
  ): Option[File] =
    options.find(_ == WithFinagle) map { _ =>
      new File(packageDir, service.sid.toTitleCase.name + "$FinagleService" + fileExtension)
    }
}
