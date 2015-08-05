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

import java.io.File

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

  override def normalizeCase[N <: Node](node: N): N = {
    (node match {
      case e: EnumField =>
        e.copy(sid = e.sid.toUpperCase)
      case _ => super.normalizeCase(node)
    }).asInstanceOf[N]
  }

  def genList(
    list: ListRHS,
    fieldType: Option[FieldType] = None
  ): CodeFragment = {
    val listElemType = fieldType.map(_.asInstanceOf[ListType].eltType)
    val code =
      list.elems.map { e =>
        genConstant(e, listElemType).toData
      }.mkString(", ")
    v(s"Utilities.makeList($code)")
  }

  def genSet(
    set: SetRHS,
    fieldType: Option[FieldType] = None
  ): CodeFragment = {
    val makeSetMethod = fieldType match {
      case Some(SetType(EnumType(_, _), _)) => "makeEnumSet"
      case _ => "makeSet"
    }

    val setElemType = fieldType.map(_.asInstanceOf[SetType].eltType)
    val code = set.elems.map { e =>
      genConstant(e, setElemType).toData
    }.mkString(", ")

    v(s"Utilities.$makeSetMethod($code)")
  }

  def genMap(
    map: MapRHS,
    fieldType: Option[FieldType] = None
  ): CodeFragment = {
    val mapType = fieldType.map(_.asInstanceOf[MapType])
    val code = map.elems.map { case (k, v) =>
      val key = genConstant(k, mapType.map(_.keyType)).toData
      val value = genConstant(v, mapType.map(_.valueType)).toData
      s"Utilities.makeTuple($key, $value)"
    }.mkString(", ")

    v(s"Utilities.makeMap($code)")
  }

  def genEnum(enum: EnumRHS, fieldType: Option[FieldType] = None): CodeFragment = {
    def getTypeId: Identifier = fieldType.getOrElse(Void) match {
      case n: NamedType => qualifyNamedType(n)
      case _ =>  enum.enum.sid
    }
    genID(enum.value.sid.toUpperCase.addScope(getTypeId.toTitleCase))
  }

  def genStruct(struct: StructRHS): CodeFragment = {
    val code = "new " + struct.sid.name + ".Builder()" + 
        struct.elems.map { case (field, rhs) => 
          "." + field.sid.name + "(" + genConstant(rhs) + ")"
         }.mkString("") +".build()"
     v(code)
  }

  // TODO
  def genUnion(union: UnionRHS): CodeFragment = ???

  override def genDefaultValue(fieldType: FieldType): CodeFragment = {
    val code = fieldType match {
      case MapType(_, _, _) => "Utilities.makeMap()"
      case SetType(eltType: EnumType, _) =>
        s"Utilities.makeEnumSet(${genType(eltType)}.class)"
      case SetType(eltType, _) =>
        "Utilities.makeSet()"
      case ListType(_, _) => "Utilities.makeList()"
      case TI64 => "0L"
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

  def genType(t: FunctionType, namespace: Option[Identifier]): CodeFragment = {
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
    v(code)
  }

  def genPrimitiveType(t: FunctionType): CodeFragment = {
    val code = t match {
      case Void => "void"
      case TBool => "boolean"
      case TByte => "byte"
      case TI16 => "short"
      case TI32 => "int"
      case TI64 => "long"
      case TDouble => "double"
      case _ => genType(t).toData
    }
    v(code)
  }

  def genFieldType(f: Field): CodeFragment = {
    val code = if (f.requiredness.isOptional) {
      val baseType = genType(f.fieldType).toData
      "com.twitter.scrooge.Option<" + baseType + ">"
    } else {
      genPrimitiveType(f.fieldType).toData
    }
    v(code)
  }

  def genFieldParams(fields: Seq[Field], asVal: Boolean = false): CodeFragment = {
    val code = fields.map {
      f =>
        genFieldType(f).toData + " " + genID(f.sid).toData
    }.mkString(", ")
    v(code)
  }

  def genBaseFinagleService = v("Service<byte[], byte[]>")

  def getParentFinagleService(p: ServiceParent): CodeFragment =
    genID(SimpleID("FinagledService").addScope(getServiceParentID(p)))

  def getParentFinagleClient(p: ServiceParent): CodeFragment =
    genID(SimpleID("FinagledClient").addScope(getServiceParentID(p)))

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
