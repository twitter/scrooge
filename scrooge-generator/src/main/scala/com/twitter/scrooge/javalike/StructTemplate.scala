package com.twitter.scrooge
package javalike

/**
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
import com.twitter.handlebar.Dictionary
import com.twitter.handlebar.Dictionary._

trait StructTemplate extends Generator { self: JavaLike =>
  case class Binding[FT <: FieldType](name: String, fieldType: FT)

  import Dictionary._

  // ----- readers

  lazy val readBasicTemplate = templates("readBasic").generate { binding: Binding[BaseType] =>
    val Binding(name, t) = binding
    Dictionary(
      "type" -> v(typeName(t)),
      "name" -> v(name),
      "protocolReadMethod" -> v(protocolReadMethod(t))
    )
  }

  def readListTemplate = templates("readList").generate { binding: Binding[ListType] =>
    val Binding(name, t) = binding
    Dictionary(
      // FIXME remove corba "elt" jargon
      "name" -> v(name),
      "eltType" -> v(typeName(t.eltType)),
      "eltReader" -> v(readTemplate(Binding("element", t.eltType)))
    )
  }

  def readSetTemplate = templates("readSet").generate { binding: Binding[SetType] =>
    val Binding(name, t) = binding
    Dictionary(
      "name" -> v(name),
      "eltType" -> v(typeName(t.eltType)),
      "eltReader" -> v(readTemplate(Binding("element", t.eltType)))
    )
  }

  lazy val readMapTemplate = templates("readMap").generate { binding: Binding[MapType] =>
    val Binding(name, t) = binding
    Dictionary(
      "name" -> v(name),
      "keyType" -> v(typeName(t.keyType)),
      "valueType" -> v(typeName(t.valueType)),
      "keyReader" -> v(readTemplate(Binding("key", t.keyType))),
      "valueReader" -> v(readTemplate(Binding("value", t.valueType)))
    )
  }

  lazy val readStructTemplate = templates("readStruct").generate { binding: Binding[StructType] =>
    val Binding(name, t) = binding
    Dictionary(
      "name" -> v(name),
      "type" -> v(t.name)
    )
  }

  lazy val readEnumTemplate = templates("readEnum").generate { binding: Binding[EnumType] =>
    val Binding(name, t) = binding
    Dictionary(
      "name" -> v(name),
      "type" -> v(t.name)
    )
  }

  def readTemplate[T <: FieldType](binding: Binding[T]): String = {
    val Binding(name, t) = binding
    t match {
      case x: ListType   => readListTemplate(Binding(name, x))
      case x: SetType    => readSetTemplate(Binding(name, x))
      case x: MapType    => readMapTemplate(Binding(name, x))
      case x: StructType => readStructTemplate(Binding(name, x))
      case x: EnumType   => readEnumTemplate(Binding(name, x))
      case x: BaseType   => readBasicTemplate(Binding(name, x))
    }
  }

  lazy val readFieldTemplate = templates("readField").generate { f: Field =>
    Dictionary(
      "id"          -> v(f.id.toString),
      "name"        -> v(f.name),
      "constType"   -> v(constType(f.`type`)),
      "optionality" -> v(if (f.requiredness.isOptional) "Some" else ""),
      "valueReader" -> v(readTemplate(Binding(f.name, f.`type`))),
      "required"    -> v(f.requiredness.isRequired)
    )
  }

  // ----- writers

  lazy val writeBasicTemplate = templates("writeBasic").generate { binding: Binding[BaseType] =>
    val Binding(name, t) = binding
    Dictionary(
      "name" -> v(name),
      "protocolWriteMethod" -> v(protocolWriteMethod(t))
    )
  }

  lazy val writeListTemplate = templates("writeList").generate { binding: Binding[ListType] =>
    val Binding(name, t) = binding
    Dictionary(
      "name" -> v(name),
      "eltType" -> v(constType(t.eltType)),
      "eltWriter" -> v(writeTemplate(Binding("element", t.eltType)))
    )
  }

  lazy val writeSetTemplate = templates("writeSet").generate { binding: Binding[SetType] =>
    val Binding(name, t) = binding
    val eltName = "_element"
    Dictionary(
      "name" -> v(name),
      "eltName" -> eltName,
      "eltType" -> v(constType(t.eltType)),
      "eltWriter" -> v(writeTemplate(Binding(eltName, t.eltType)))
    )
  }

  lazy val writeMapTemplate = templates("writeMap").generate { binding: Binding[MapType] =>
    val Binding(name, t) = binding
    val keyName = "_key"
    val valueName = "_value"
    Dictionary(
      "name" -> v(name),
      "keyType" -> v(constType(t.keyType)),
      "valueType" -> v(constType(t.valueType)),
      "keyName" -> keyName,
      "valueName" -> valueName,
      "keyWriter" -> v(writeTemplate(Binding(keyName, t.keyType))),
      "valueWriter" -> v(writeTemplate(Binding(valueName, t.valueType)))
    )
  }

  lazy val writeStructTemplate = templates("writeStruct").generate { binding: Binding[StructType] =>
    val Binding(name, _) = binding
    Dictionary("name" -> name)
  }

  lazy val writeEnumTemplate = templates("writeEnum").generate { binding: Binding[EnumType] =>
    val Binding(name, _) = binding
    Dictionary("name" -> name)
  }

  def writeTemplate[T <: FieldType](binding: Binding[T]): String = {
    val Binding(name, t) = binding
    t match {
      case x: ListType => writeListTemplate(Binding(name, x))
      case x: SetType => writeSetTemplate(Binding(name, x))
      case x: MapType => writeMapTemplate(Binding(name, x))
      case x: StructType => writeStructTemplate(Binding(name, x))
      case x: EnumType => writeEnumTemplate(Binding(name, x))
      case x: BaseType => writeBasicTemplate(Binding(name, x))
    }
  }

  // FIXME optional/nullable/primitive
  lazy val writeFieldTemplate = templates("writeField").generate { f: Field =>
    val conditional = if (f.requiredness.isOptional) {
      "`" + f.name + "`.isDefined"
    } else {
      f.`type` match {
        case AST.TBool | AST.TByte | AST.TI16 | AST.TI32 | AST.TI64 | AST.TDouble =>
          "true"
        case _ =>
          "`" + f.name + "` ne null"
      }
    }
    Dictionary(
      "name" -> v(f.name),
      "type" -> v(typeName(f.`type`)),
      "conditional" -> v(conditional),
      "fieldConst" -> v(writeFieldConst(f.name)),
      "getter" -> v(if (f.requiredness.isOptional) ".get" else ""),
      "valueWriter" -> v(writeTemplate(Binding(f.name, f.`type`)))
    )
  }

  // ----- struct

  lazy val structTemplate = templates("struct").generate { pair: (Option[String], StructLike) =>
    val (namespace, struct) = pair
    val fieldDictionaries = struct.fields.zipWithIndex map {
      case (field, index) =>
        Dictionary(
          "index" -> v(index.toString),
          "indexP1" -> v((index + 1).toString),
          "name" -> v(field.name),
          "Name" -> v(TitleCase(field.name)),
          "id" -> v(field.id.toString),
          "fieldConst" -> v(writeFieldConst(field.name)),
          "constType" -> v(constType(field.`type`)),
          "fieldType" -> v(fieldTypeName(field, mutable = false)),
          "hasDefaultValue" -> v(defaultFieldValue(field).isDefined),
          "defaultFieldValue" -> v(defaultFieldValue(field).getOrElse("")),
          "defaultReadValue" -> v(defaultReadValue(field)),
          "required" -> v(field.requiredness.isRequired),
          "optional" -> {
            if (!field.requiredness.isOptional) {
              v(false)
            } else {
              v(List(Dictionary("elementType" -> v(typeName(field.`type`)))))
            }
          },
          "nullable" -> v(isNullableType(field.`type`, field.requiredness.isOptional)),
          "collection" -> v {
            field.`type` match {
              case ListType(eltType, _) => List(
                Dictionary(
                  "elementType" -> v(typeName(eltType))
                )
              )
              case SetType(eltType, _) => List(
                Dictionary(
                  "elementType" -> v(typeName(eltType))
                )
              )
              case MapType(keyType, valueType, _) => List(
                Dictionary(
                  "elementType" ->
                    v("(" + typeName(keyType) + ", " + typeName(valueType) + ")")
                )
              )
              case _ => Nil
            }
          },
          "reader" -> v(readFieldTemplate(field)),
          "writer" -> v(writeFieldTemplate(field)),
          "toImmutable" -> v(toImmutable(field)),
          "toMutable" -> v {
            toMutable(field) match {
              case (prefix, suffix) => Seq(Dictionary("prefix" -> v(prefix), "suffix" -> v(suffix)))
            }
          },
          "struct" -> v(struct.name),
          "comma" -> v(if (index == struct.fields.size - 1) "" else ",")
        )
    }
    val parentType = struct match {
      case _: AST.Exception_ => "SourcedException with ThriftStruct"
      case _ => "ThriftStruct"
    }
    val arity = struct.fields.size
    val product = if (arity >= 1 && arity <= 22) {
      val fieldTypes = struct.fields.map {
        f => fieldTypeName(f)
      }.mkString(", ")
      "Product" + arity + "[" + fieldTypes + "]"
    } else {
      "Product"
    }
    Dictionary(
      "public" -> v(namespace.isDefined),
      "package" -> v(namespace.getOrElse("")),
      "name" -> v(struct.name),
      "parentType" -> v(parentType),
      "fields" -> v(fieldDictionaries),
      "arity" -> arity.toString,
      "product" -> v(product),
      "arity0" -> v(arity == 0),
      "arity1" -> (if (arity == 1) fieldDictionaries.take(1) else Nil),
      "arityN" -> v(arity > 1 && arity <= 22),
      "withProxy" -> v(struct.isInstanceOf[Struct])
    )
  }
}
