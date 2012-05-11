package com.twitter.scrooge
package javagen

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

import AST._
import javalike.JavaLike

class JavaGenerator extends JavaLike {
  val fileExtension = ".java"
  val templateDirName = "/javagen/"

  def namespace(doc: Document) =
    doc.namespace("java") getOrElse("thrift")

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
        e.copy(name = UpperCase(e.name))
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
    (if (mutable) "Utilities.makeList(" else "Utilities.makeList(") +
      list.elems.map(constantValue(_)).mkString(", ") + ")"
  }

  def setValue(set: SetConstant, mutable: Boolean = false): String = {
    (if (mutable) "Utilities.makeSet(" else "Utilities.makeSet(") +
      set.elems.map(constantValue(_)).mkString(", ") + ")"
  }

  def mapValue(map: MapConstant, mutable: Boolean = false): String = {
    (if (mutable) "Utilities.makeMap(" else "Utilities.makeMap(") + (map.elems.map {
      case (k, v) =>
        "Utilities.makeTuple(" + constantValue(k) + ", " + constantValue(v) + ")"
    } mkString (", ")) + ")"
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
  def toMutable(t: FieldType): (String, String) = {
    t match {
      case MapType(_, _, _) | SetType(_, _) => (typeName(t, true) + "() ++= ", "")
      case ListType(_, _) => ("", ".toBuffer")
      case _ => ("", "")
    }
  }

  override def defaultValue(`type`: FieldType, mutable: Boolean = false) = {
    `type` match {
      case MapType(_, _, _) => "Utilities.makeMap()"
      case SetType(_, _) => "Utilities.makeSet()"
      case ListType(_, _) => "Utilities.makeList()"
      case TI64 => "(long) 0"
      case _ => super.defaultValue(`type`, mutable)
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
      case Void => "Void"
      case TBool => "Boolean"
      case TByte => "Byte"
      case TI16 => "Short"
      case TI32 => "Integer"
      case TI64 => "Long"
      case TDouble => "Double"
      case TString => "String"
      case TBinary => "ByteBuffer"
      case MapType(k, v, _) => "Map<" + typeName(k) + ", " + typeName(v) + ">"
      case SetType(x, _) => "Set<" + typeName(x) + ">"
      case ListType(x, _) => "List<" + typeName(x) + ">"
      case n: NamedType => n.name
      case r: ReferenceType => r.name
    }
  }

  def primitiveTypeName(t: FunctionType, mutable: Boolean = false): String = {
    t match {
      case Void => "void"
      case TBool => "boolean"
      case TByte => "byte"
      case TI16 => "short"
      case TI32 => "int"
      case TI64 => "long"
      case TDouble => "double"
      case _ => typeName(t, mutable)
    }
  }

  def fieldTypeName(f: Field, mutable: Boolean = false): String = {
    if (f.requiredness.isOptional) {
      val baseType = typeName(f.`type`, mutable)
      "Utilities.Option<" + baseType + ">"
    } else {
      primitiveTypeName(f.`type`)
    }
  }

  def fieldParams(fields: Seq[Field], asVal: Boolean = false): String = {
    fields.map { f =>
      fieldTypeName(f) + " " + f.name
    }.mkString(", ")
  }
}
