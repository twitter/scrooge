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

import com.twitter.scrooge.ast._
import com.twitter.scrooge.frontend.{ScroogeInternalException, ResolvedDocument}
import com.twitter.scrooge.mustache.Dictionary._
import com.twitter.scrooge.mustache.HandlebarLoader

object JavaGeneratorFactory extends GeneratorFactory {
  val lang = "experimental-java"
  val handlebarLoader = new HandlebarLoader("/javagen/", ".java")
  def apply(
    includeMap: Map[String, ResolvedDocument],
    defaultNamespace: String,
    experimentFlags: Seq[String]
  ): ThriftGenerator = new JavaGenerator(includeMap, defaultNamespace, handlebarLoader)
}

class JavaGenerator(
  val includeMap: Map[String, ResolvedDocument],
  val defaultNamespace: String,
  val templatesLoader: HandlebarLoader
) extends TemplateGenerator {
  def templates: HandlebarLoader = templatesLoader

  val fileExtension = ".java"
  val experimentFlags = Seq.empty[String]

  private[this] object JavaKeywords {
    private[this] val set = Set[String](
      "abstract", "assert", "boolean", "break", "byte", "case",
      "catch", "char", "class", "const", "continue", "default", "double",
      "do", "else", "enum", "extends", "false", "final", "finally", "float",
      "for", "goto", "if", "implements", "import", "instanceof", "int",
      "interface", "long", "native", "new", "null", "package", "private",
      "protected", "public", "return", "short", "static", "strictfp",
      "super", "switch", "synchronized", "this", "throw", "throws",
      "transient", "true", "try", "void", "volatile", "while")
    def contains(str: String): Boolean = set.contains(str)
  }

  // put Java keywords in "_"
  def quoteKeyword(str: String): String =
    if (JavaKeywords.contains(str))
      "_" + str + "_"
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
          funcName = f.funcName.toCamelCase,
          args = f.args.map(normalizeCase(_)),
          throws = f.throws.map(normalizeCase(_)))
      case c: ConstDefinition =>
        c.copy(value = normalizeCase(c.value))
      case e: Enum =>
        e.copy(values = e.values.map(normalizeCase(_)))
      case e: EnumField =>
        e.copy(sid = e.sid.toUpperCase)
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

  def genList(list: ListRHS, mutable: Boolean = false): CodeFragment = {
    val code = (if (mutable) "Utilities.makeList(" else "Utilities.makeList(") +
      list.elems.map(genConstant(_).toData).mkString(", ") + ")"
    codify(code)
  }

  def genSet(set: SetRHS, mutable: Boolean = false): CodeFragment = {
    val code = (if (mutable) "Utilities.makeSet(" else "Utilities.makeSet(") +
      set.elems.map(genConstant(_).toData).mkString(", ") + ")"
    codify(code)
  }

  def genMap(map: MapRHS, mutable: Boolean = false): CodeFragment = {
    val code = (if (mutable) "Utilities.makeMap(" else "Utilities.makeMap(") + (map.elems.map {
      case (k, v) =>
        "Utilities.makeTuple(" + genConstant(k).toData + ", " + genConstant(v).toData + ")"
    } mkString (", ")) + ")"
    codify(code)
  }

  def genEnum(enum: EnumRHS, fieldType: Option[FieldType] = None): CodeFragment = {
    def getTypeId: Identifier = fieldType.getOrElse(Void) match {
      case n: NamedType => qualifyNamedType(n)
      case _ =>  enum.enum.sid
    }
    genID(enum.value.sid.toUpperCase.addScope(getTypeId.toTitleCase))
  }

  // TODO
  def genStruct(struct: StructRHS): CodeFragment =
    throw new Exception("not implemented")

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

  override def genDefaultValue(fieldType: FieldType, mutable: Boolean = false): CodeFragment = {
    val code = fieldType match {
      case MapType(_, _, _) => "Utilities.makeMap()"
      case SetType(_, _) => "Utilities.makeSet()"
      case ListType(_, _) => "Utilities.makeList()"
      case TI64 => "0L"
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

  def genType(t: FunctionType, mutable: Boolean = false): CodeFragment = {
    val code = t match {
      case Void => "Void"
      case OnewayVoid => "Void"
      case TBool => "Boolean"
      case TByte => "Byte"
      case TI16 => "Short"
      case TI32 => "Integer"
      case TI64 => "Long"
      case TDouble => "Double"
      case TString => "String"
      case TBinary => "ByteBuffer"
      case MapType(k, v, _) => "Map<" + genType(k).toData + ", " + genType(v).toData + ">"
      case SetType(x, _) => "Set<" + genType(x).toData + ">"
      case ListType(x, _) => "List<" + genType(x).toData + ">"
      case n: NamedType => genID(qualifyNamedType(n).toTitleCase).toData
      case r: ReferenceType =>
        throw new ScroogeInternalException("ReferenceType should not appear in backend")
    }
    codify(code)
  }

  def genPrimitiveType(t: FunctionType, mutable: Boolean = false): CodeFragment = {
    val code = t match {
      case Void => "void"
      case TBool => "boolean"
      case TByte => "byte"
      case TI16 => "short"
      case TI32 => "int"
      case TI64 => "long"
      case TDouble => "double"
      case _ => genType(t, mutable).toData
    }
    codify(code)
  }

  def genFieldType(f: Field, mutable: Boolean = false): CodeFragment = {
    val code = if (f.requiredness.isOptional) {
      val baseType = genType(f.fieldType, mutable).toData
      "com.twitter.scrooge.Option<" + baseType + ">"
    } else {
      genPrimitiveType(f.fieldType).toData
    }
    codify(code)
  }

  def genFieldParams(fields: Seq[Field], asVal: Boolean = false): CodeFragment = {
    val code = fields.map {
      f =>
        genFieldType(f).toData + " " + genID(f.sid).toData
    }.mkString(", ")
    codify(code)
  }

  def genBaseFinagleService = codify("Service<byte[], byte[]>")

  def getParentFinagleService(p: ServiceParent): CodeFragment =
    genID(SimpleID("FinagledService").addScope(getServiceParentID(p)))

  def getParentFinagleClient(p: ServiceParent): CodeFragment =
    genID(SimpleID("FinagledClient").addScope(getServiceParentID(p)))
}
