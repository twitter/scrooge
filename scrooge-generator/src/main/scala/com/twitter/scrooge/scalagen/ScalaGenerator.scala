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

package com.twitter.scrooge
package scalagen

import AST._
import javalike.JavaLike

class ScalaGenerator extends JavaLike {
  val fileExtension = ".scala"
  val templateDirName = "/scalagen/"

  def namespace(doc: Document) =
    doc.namespace("scala") orElse doc.namespace("java") getOrElse("thrift")

  def normalizeCase[N <: AST.Node](node: N) = {
    (node match {
      case d: Document =>
        d.copy(defs = d.defs.map(normalizeCase(_)))
      case i: Identifier =>
        i.copy(name = TitleCase(i.name))
      case e: EnumValueConstant =>
        e.copy(normalizeCase(e.enum), normalizeCase(e.value))
      case f: Field =>
        f.copy(
          name = CamelCase(f.name),
          default = f.default.map(normalizeCase(_)))
      case f: Function =>
        f.copy(
          localName = CamelCase(f.localName),
          args = f.args.map(normalizeCase(_)),
          throws = f.throws.map(normalizeCase(_)))
      case c: Const =>
        c.copy(value = normalizeCase(c.value))
      case e: Enum =>
        e.copy(values = e.values.map(normalizeCase(_)))
      case e: EnumValue =>
        e.copy(name = TitleCase(e.name))
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

  def listValue(list: ListConstant, mutable: Boolean = false): String = {
    (if (mutable) "mutable.Buffer(" else "Seq(") +
      list.elems.map(constantValue(_)).mkString(", ") + ")"
  }

  def setValue(set: SetConstant, mutable: Boolean = false): String = {
    (if (mutable) "mutable.Set(" else "Set(") +
      set.elems.map(constantValue(_)).mkString(", ") + ")"
  }

  def mapValue(map: MapConstant, mutable: Boolean = false): String = {
    (if (mutable) "mutable.Map(" else "Map(") + (map.elems.map {
      case (k, v) =>
        constantValue(k) + " -> " + constantValue(v)
    } mkString(", ")) + ")"
  }

  override def defaultValue(`type`: FieldType, mutable: Boolean = false) = {
    `type` match {
      case TI64 => "0L"
      case MapType(_, _, _) | SetType(_, _) | ListType(_, _) =>
        typeName(`type`, mutable) + "()"
      case _ => super.defaultValue(`type`, mutable)
    }
  }

  /**
   * Generates a suffix to append to a field expression that will
   * convert the value to an immutable equivalent.
   */
  def toImmutable(t: FieldType): String = {
    t match {
      case MapType(_, _, _) => ".toMap"
      case SetType(_, _) => ".toSet"
      case ListType(_, _) => ".toList"
      case _ => ""
    }
  }

  /**
   * Generates a suffix to append to a field expression that will
   * convert the value to an immutable equivalent.
   */
  def toImmutable(f: Field): String = {
    if (f.requiredness.isOptional) {
      toImmutable(f.`type`) match {
        case "" => ""
        case underlyingToImmutable => ".map(_" + underlyingToImmutable + ")"
      }
    } else {
      toImmutable(f.`type`)
    }
  }

  /**
   * Generates a prefix and suffix to wrap around a field expression that will
   * convert the value to a mutable equivalent.
   */
  def toMutable(t: FieldType): (String, String)  = {
    t match {
      case MapType(_, _, _) | SetType(_, _) => (typeName(t, true) + "() ++= ", "")
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
      toMutable(f.`type`) match {
        case ("", "") => ("", "")
        case (prefix, suffix) => ("", ".map(" + prefix + "_" + suffix + ")")
      }
    } else {
      toMutable(f.`type`)
    }
  }

  def typeName(t: FunctionType, mutable: Boolean = false): String = {
    t match {
      case Void => "Unit"
      case TBool => "Boolean"
      case TByte => "Byte"
      case TI16 => "Short"
      case TI32 => "Int"
      case TI64 => "Long"
      case TDouble => "Double"
      case TString => "String"
      case TBinary => "ByteBuffer"
      case MapType(k, v, _) =>
        (if (mutable) "mutable." else "") + "Map[" + typeName(k) + ", " + typeName(v) + "]"
      case SetType(x, _) =>
        (if (mutable) "mutable." else "") + "Set[" + typeName(x) + "]"
      case ListType(x, _) =>
        (if (mutable) "mutable.Buffer" else "Seq") + "[" + typeName(x) + "]"
      case n: NamedType => n.prefix.map("_" + _ + "_.").getOrElse("") + n.name
      case r: ReferenceType => r.name
    }
  }

  def primitiveTypeName(t: FunctionType, mutable: Boolean = false) = typeName(t, mutable)

  def fieldTypeName(f: Field, mutable: Boolean = false): String = {
    val baseType = typeName(f.`type`, mutable)
    if (f.requiredness.isOptional) {
      "Option[" + baseType + "]"
    } else {
      baseType
    }
  }

  def fieldParams(fields: Seq[Field], asVal: Boolean = false): String = {
    fields.map { f =>
      val valPrefix = if (asVal) "val " else ""
      val nameAndType = "`" + f.name + "`: " + fieldTypeName(f)
      val defaultValue = defaultFieldValue(f) map { " = " + _ }
      valPrefix + nameAndType + defaultValue.getOrElse("")
    }.mkString(", ")
  }
}
