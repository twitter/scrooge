package com.twitter.scrooge.backend

/**
 * Copyright 2011 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.twitter.scrooge.ast._
import com.twitter.scrooge.mustache.Dictionary
import com.twitter.scrooge.ScroogeInternalException
import com.twitter.scrooge.mustache.Dictionary._

trait StructTemplate {
  self: Generator =>

  case class Binding[FT <: FieldType](name: String, fieldType: FT)

  val TypeTemplate =
    Dictionary(
      "isList" -> v(false),
      "isSet" -> v(false),
      "isMap" -> v(false),
      "isStruct" -> v(false),
      "isEnum" -> v(false),
      "isBase" -> v(false))

  def readWriteInfo[T <: FieldType](sid: SimpleID, t: FieldType): Dictionary = {
    t match {
      case t: ListType =>
        val elt = sid.append("_element")
        TypeTemplate + Dictionary(
          "isList" -> v(Dictionary(
            "name" -> genID(sid),
            "eltName" -> genID(elt),
            "eltConstType" -> genConstType(t.eltType),
            "eltType" -> genType(t.eltType),
            "eltReadWriteInfo" -> v(readWriteInfo(elt, t.eltType))
          )))
      case t: SetType =>
        val elt =  sid.append("_element")
        TypeTemplate + Dictionary(
          "isSet" -> v(Dictionary(
            "name" -> genID(sid),
            "eltName" -> genID(elt),
            "eltConstType" -> genConstType(t.eltType),
            "eltType" -> genType(t.eltType),
            "eltReadWriteInfo" -> v(readWriteInfo(elt, t.eltType))
          )))
      case t: MapType =>
        val key =  sid.append("_key")
        val value =  sid.append("_value")
        TypeTemplate + Dictionary(
          "isMap" -> v(Dictionary(
            "name" -> genID(sid),
            "keyConstType" -> genConstType(t.keyType),
            "valueConstType" -> genConstType(t.valueType),
            "keyType" -> genType(t.keyType),
            "valueType" -> genType(t.valueType),
            "keyName" -> genID(key),
            "valueName" -> genID(value),
            "keyReadWriteInfo" -> v(readWriteInfo(key, t.keyType)),
            "valueReadWriteInfo" -> v(readWriteInfo(value, t.valueType))
          )))
      case t: StructType =>
        TypeTemplate + Dictionary(
          "isStruct" -> v(Dictionary(
            "name" -> genID(sid),
            "fieldType" -> genType(t)
          )))
      case t: EnumType =>
        TypeTemplate + Dictionary(
          "isEnum" -> v(Dictionary(
            "name" -> genID(sid),
            "fieldType" -> genType(t)
          )))
      case t: BaseType =>
        TypeTemplate + Dictionary(
          "isBase" -> v(Dictionary(
            "type" -> genType(t),
            "name" -> genID(sid),
            "protocolWriteMethod" -> genProtocolWriteMethod(t),
            "protocolReadMethod" -> genProtocolReadMethod(t)
          )))
      case t: ReferenceType =>
        throw new ScroogeInternalException("ReferenceType should have been resolved by now")
    }
  }

  def fieldsToDict(fields: Seq[Field], blacklist: Seq[String]) = {
    fields.zipWithIndex map {
      case (field, index) =>
        val valueVariableID = field.sid.append("_item")
        Dictionary(
          "index" -> codify(index.toString),
          "indexP1" -> codify((index + 1).toString),
          "_fieldName" -> genID(field.sid.prepend("_")), // for Java only
          "unsetName" -> genID(field.sid.toTitleCase.prepend("unset")), // for Java only
          "getName" -> genID(field.sid.toTitleCase.prepend("get")), // for Java only
          "isSetName" -> genID(field.sid.toTitleCase.prepend("isSet")), // for Java only
          "fieldName" -> genID(field.sid),
          "fieldNameForWire" -> codify(field.originalName),
          "newFieldName" -> genID(field.sid.toTitleCase.prepend("new")),
          "FieldName" -> genID(field.sid.toTitleCase),
          "FIELD_NAME" -> genID(field.sid.toUpperCase),
          "gotName" -> genID(field.sid.prepend("_got_")),
          "id" -> codify(field.index.toString),
          "fieldConst" -> genID(field.sid.toTitleCase.append("Field")),
          "constType" -> genConstType(field.fieldType),
          "isPrimitive" -> v(isPrimitive(field.fieldType)),
          "primitiveFieldType" -> genPrimitiveType(field.fieldType, mutable = false),
          "fieldType" -> genType(field.fieldType, mutable = false),
          "isImported" -> v(field.fieldType match {
            case n: NamedType => n.scopePrefix.isDefined
            case _ => false
          }),
          "isNamedType" -> v(field.fieldType.isInstanceOf[NamedType]),
          // "qualifiedFieldType" is used to generate qualified type name even if it's not
          // imported, in case other same-named entities are generated in the same file.
          "qualifiedFieldType" -> v(templates("qualifiedFieldType")),
          "hasDefaultValue" -> v(genDefaultFieldValue(field).isDefined),
          "defaultFieldValue" -> genDefaultFieldValue(field).getOrElse(NoValue),
          "defaultReadValue" -> genDefaultReadValue(field),
          "hasGetter" -> v(!blacklist.contains(field.sid.name)),
          "hasIsDefined" -> v(field.requiredness.isOptional || (!field.requiredness.isRequired && !isPrimitive(field.fieldType))),
          "required" -> v(field.requiredness.isRequired),
          "optional" -> v(field.requiredness.isOptional),
          "nullable" -> v(isNullableType(field.fieldType, field.requiredness.isOptional)),
          "collection" -> v {
            field.fieldType match {
              case ListType(eltType, _) => List(
                Dictionary(
                  "elementType" -> genType(eltType)
                )
              )
              case SetType(eltType, _) => List(
                Dictionary(
                  "elementType" -> genType(eltType)
                )
              )
              case MapType(keyType, valueType, _) => List(
                Dictionary(
                  "elementType" ->
                    codify("(" + genType(keyType).toData + ", " + genType(valueType).toData + ")")
                )
              )
              case _ => Nil
            }
          },
          "readField" -> v(templates("readField")),
          "readUnionField" -> v(templates("readUnionField")),
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
          "readWriteInfo" -> v(readWriteInfo(valueVariableID, field.fieldType)),
          "toImmutable" -> genToImmutable(field),
          "toMutable" -> v {
            toMutable(field) match {
              case (prefix, suffix) => Seq(Dictionary(
                "prefix" -> codify(prefix),
                "suffix" -> codify(suffix)))
            }
          },
          "valueVariableName" -> genID(valueVariableID)
        )
    }
  }

  private def exceptionMsgFieldName(struct: StructLike): Option[SimpleID] = {
    val msgField: Option[Field] = struct.fields find {
      field =>
      // 1st choice: find a field called message
        field.sid.name == "message"
    } orElse {
      // 2nd choice: the first string field
      struct.fields find {
        field => field.fieldType == TString
      }
    }

    msgField map {
      _.sid
    }
  }

  def structDict(
    struct: StructLike,
    namespace: Option[Identifier],
    includes: Seq[Include],
    serviceOptions: Set[ServiceOption]
  ) = {
    val isException = struct.isInstanceOf[Exception_]
    val parentType = if (isException) {
      if (serviceOptions contains WithFinagleClient) {
        "ThriftException with SourcedException with ThriftStruct"
      } else {
        "ThriftException with ThriftStruct"
      }
    } else {
      "ThriftStruct"
    }
    val arity = struct.fields.size
    val product = if (arity >= 1 && arity <= 22) {
      val fieldTypes = struct.fields.map {
        f => genFieldType(f).toData
      }.mkString(", ")
      "Product" + arity + "[" + fieldTypes + "]"
    } else {
      "Product"
    }

    val exceptionMsgField: Option[SimpleID] = if (isException) exceptionMsgFieldName(struct) else None

    val fieldDictionaries = fieldsToDict(
      struct.fields,
      if (isException) Seq("message") else Seq())

    val isPublic = namespace.isDefined

    Dictionary(
      "public" -> v(isPublic),
      "package" -> namespace.map{ genID(_) }.getOrElse(codify("")),
      "docstring" -> codify(struct.docstring.getOrElse("")),
      "parentType" -> codify(parentType),
      "fields" -> v(fieldDictionaries),
      "defaultFields" -> v(fieldsToDict(struct.fields.filter(!_.requiredness.isOptional), Seq())),
      "alternativeConstructor" -> v(
        struct.fields.exists(_.requiredness.isOptional) && struct.fields.exists(_.requiredness.isDefault)),
      "StructNameForWire" -> codify(struct.originalName),
      "StructName" ->
        // if isPublic, the struct comes from a Thrift definition. Otherwise
        // it's an internal struct: fooMethod$args or fooMethod$result
        (if (isPublic) genID(struct.sid.toTitleCase) else genID(struct.sid)),
      "underlyingStructName" -> genID(struct.sid.prepend("_underlying_")),
      "arity" -> codify(arity.toString),
      "isException" -> v(isException),
      "hasExceptionMessage" -> v(exceptionMsgField.isDefined),
      "exceptionMessageField" -> exceptionMsgField.map { genID(_) }.getOrElse { codify("")},
      "product" -> codify(product),
      "arity0" -> v(arity == 0),
      "arity1" -> v((if (arity == 1) fieldDictionaries.take(1) else Nil)),
      "arityN" -> v(arity > 1 && arity <= 22),
      "withProxy" -> v(struct.isInstanceOf[Struct]),
      "withFinagleClient" -> v(serviceOptions contains WithFinagleClient),
      "date" -> codify(generationDate),
      "withSkipNullWrite" -> v(serviceOptions contains WithSkipNullWrite)
    )
  }
}
