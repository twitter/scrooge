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

trait StructTemplate extends Generator { self: JavaLike =>
  case class Binding[FT <: FieldType](name: String, fieldType: FT)

  import Dictionary._

  val TypeTemplate =
    Dictionary(
      "isList" -> false,
      "isSet" -> false,
      "isMap" -> false,
      "isStruct" -> false,
      "isEnum" -> false,
      "isBase" -> false)

  def readWriteInfo[T <: FieldType](name: String, t: FieldType): Dictionary = {
    t match {
      case t: ListType =>
        val eltName = "_" + name + "_element"
        TypeTemplate + Dictionary(
          "isList" -> Dictionary(
            "name" -> v(name),
            "eltName" -> v(eltName),
            "eltConstType" -> v(constType(t.eltType)),
            "eltType" -> v(typeName(t.eltType)),
            "eltReadWriteInfo" -> v(readWriteInfo(eltName, t.eltType))
        ))
      case t: SetType =>
        val eltName = "_" + name + "_element"
        TypeTemplate + Dictionary(
          "isSet" -> Dictionary(
            "name" -> v(name),
            "eltName" -> eltName,
            "eltConstType" -> v(constType(t.eltType)),
            "eltType" -> v(typeName(t.eltType)),
            "eltReadWriteInfo" -> v(readWriteInfo(eltName, t.eltType))
          ))
      case t: MapType =>
        val keyName = "_" + name + "_key"
        val valueName = "_" + name + "_value"
        TypeTemplate + Dictionary(
          "isMap" -> Dictionary(
            "name" -> v(name),
            "keyConstType" -> v(constType(t.keyType)),
            "valueConstType" -> v(constType(t.valueType)),
            "keyType" -> v(typeName(t.keyType)),
            "valueType" -> v(typeName(t.valueType)),
            "keyName" -> keyName,
            "valueName" -> valueName,
            "keyReadWriteInfo" -> v(readWriteInfo(keyName, t.keyType)),
            "valueReadWriteInfo" -> v(readWriteInfo(valueName, t.valueType))
        ))
      case t: StructType =>
        TypeTemplate + Dictionary(
          "isStruct" -> Dictionary(
            "name" -> v(name),
            "prefix" -> v(t.prefix.getOrElse("")),
            "type" -> v(t.name)
        ))
      case t: EnumType =>
        TypeTemplate + Dictionary(
          "isEnum" -> Dictionary(
            "name" -> v(name),
            "prefix" -> v(t.prefix.getOrElse("")),
            "type" -> v(t.name)
        ))
      case t: BaseType =>
        TypeTemplate + Dictionary(
          "isBase" -> Dictionary(
            "type" -> v(typeName(t)),
            "name" -> v(name),
            "protocolWriteMethod" -> v(protocolWriteMethod(t)),
            "protocolReadMethod" -> v(protocolReadMethod(t))
        ))
    }
  }

  // ----- struct

  def structDict(
    struct: StructLike,
    myNamespace: Option[String],
    includes: Seq[Include],
    serviceOptions: Set[ServiceOption]
  ) = {
    val fieldDictionaries = struct.fields.zipWithIndex map {
      case (field, index) =>
        val valueVariableName = field.name + "_item"
        Dictionary(
          "index" -> v(index.toString),
          "indexP1" -> v((index + 1).toString),
          "isMessage" -> v(field.name == "message"),
          "isReserved" -> v(struct.isInstanceOf[AST.Exception_] && field.name == "message"),
          "name" -> v(field.name),
          "Name" -> v(TitleCase(field.name)),
          "id" -> v(field.id.toString),
          "fieldConst" -> v(writeFieldConst(field.name)),
          "constType" -> v(constType(field.`type`)),
          "primitiveFieldType" -> v(primitiveTypeName(field.`type`, mutable = false)),
          "fieldType" -> v(typeName(field.`type`, mutable = false)),
          "hasDefaultValue" -> v(defaultFieldValue(field).isDefined),
          "defaultFieldValue" -> v(defaultFieldValue(field).getOrElse("")),
          "defaultReadValue" -> v(defaultReadValue(field)),
          "required" -> v(field.requiredness.isRequired),
          "optional" -> v(field.requiredness.isOptional),
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
          "readField" -> v(templates("readField")),
          "readValue" -> v(templates("readValue")),
          "writeField" -> v(templates("writeField")),
          "writeValue" -> v(templates("writeValue")),
          "writeList" -> v(templates("writeList")),
          "writeSet" -> v(templates("writeSet")),
          "writeMap" -> v(templates("writeMap")),
          "writeStruct" -> v(templates("writeStruct")),
          "writeEnum" -> v(templates("writeEnum")),
          "writeBase" -> v(templates("writeBase")),
          "readList" -> v(templates("readList")),
          "readSet" -> v(templates("readSet")),
          "readMap" -> v(templates("readMap")),
          "readStruct" -> v(templates("readStruct")),
          "readEnum" -> v(templates("readEnum")),
          "readBase" -> v(templates("readBase")),
          "optionalType" -> v(templates("optionalType")),
          "readWriteInfo" -> v(readWriteInfo(valueVariableName, field.`type`)),
          "toImmutable" -> v(toImmutable(field)),
          "toMutable" -> v {
            toMutable(field) match {
              case (prefix, suffix) => Seq(Dictionary("prefix" -> v(prefix), "suffix" -> v(suffix)))
            }
          },
          "valueVariableName" -> v(valueVariableName),
          "struct" -> v(struct.name),
          "comma" -> v(if (index == struct.fields.size - 1) "" else ",")
        )
    }
    val parentType = struct match {
      case _: AST.Exception_ => {
        if (serviceOptions contains WithFinagleClient) {
          "SourcedException with ThriftStruct"
        } else {
          "Exception with ThriftStruct"
        }
      }
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

    val imports = includes map { include =>
      (namespace(include.document), include.prefix)
    } map {
      case (PackageName(parentPackage, subPackage), prefix) => {
        Dictionary(
          "parentPackage" -> parentPackage,
          "subPackage" -> subPackage,
          "alias" -> prefix
        )
      }
    }

    Dictionary(
      "public" -> v(myNamespace.isDefined),
      "package" -> v(myNamespace.getOrElse("")),
      "imports" -> v(imports),
      "name" -> v(struct.name),
      "parentType" -> v(parentType),
      "fields" -> v(fieldDictionaries),
      "arity" -> arity.toString,
      "isException" -> v(struct.isInstanceOf[AST.Exception_]),
      "product" -> v(product),
      "arity0" -> v(arity == 0),
      "arity1" -> (if (arity == 1) fieldDictionaries.take(1) else Nil),
      "arityN" -> v(arity > 1 && arity <= 22),
      "withProxy" -> v(struct.isInstanceOf[Struct]),
      "finagle" -> v(serviceOptions contains WithFinagleClient)
    )
  }
}
