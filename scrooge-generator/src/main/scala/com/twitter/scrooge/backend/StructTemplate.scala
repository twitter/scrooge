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
import com.twitter.scrooge.mustache.Dictionary._
import com.twitter.scrooge.frontend.ScroogeInternalException

trait StructTemplate { self: TemplateGenerator =>

  case class Binding[FT <: FieldType](name: String, fieldType: FT)

  val TypeTemplate =
    Dictionary(
      "isList" -> v(false),
      "isSet" -> v(false),
      "isMap" -> v(false),
      "isStruct" -> v(false),
      "isEnum" -> v(false),
      "isBase" -> v(false))

  def genWireConstType(t: FunctionType): CodeFragment = t match {
    case _: EnumType => codify("I32")
    case _ => genConstType(t)
  }

  def readWriteInfo[T <: FieldType](sid: SimpleID, t: FieldType): Dictionary = {
    t match {
      case t: ListType =>
        val elt = sid.append("_element")
        TypeTemplate + Dictionary(
          "isList" -> v(Dictionary(
            "name" -> genID(sid),
            "eltName" -> genID(elt),
            "eltConstType" -> genConstType(t.eltType),
            "eltWireConstType" -> genWireConstType(t.eltType),
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
            "eltWireConstType" -> genWireConstType(t.eltType),
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
            "keyWireConstType" -> genWireConstType(t.keyType),
            "valueConstType" -> genConstType(t.valueType),
            "valueWireConstType" -> genWireConstType(t.valueType),
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
          "unsetName" -> genID(field.sid.toTitleCase.prepend("unset")),
          "readName" -> genID(field.sid.toTitleCase.prepend("read")),
          "getBlobName" -> genID(field.sid.toTitleCase.prepend("get").append("Blob")),
          "readBlobName" -> genID(field.sid.toTitleCase.prepend("read").append("Blob")),
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
          "fieldKeyType" -> v(field.fieldType match {
            case MapType(keyType, _, _) => Some(genType(keyType))
            case _ => None
          }),
          "fieldValueType" -> v(field.fieldType match {
            case MapType(_, valueType, _) => Some(genType(valueType))
            case ListType(valueType, _) => Some(genType(valueType))
            case SetType(valueType, _) => Some(genType(valueType))
            case _ => None
          }),
          "fieldTypeAnnotations" -> v(StructTemplate.renderPairs(field.typeAnnotations)),
          "fieldFieldAnnotations" -> v(StructTemplate.renderPairs(field.fieldAnnotations)),
          "isImported" -> v(field.fieldType match {
            case n: NamedType => n.scopePrefix.isDefined
            case _ => false
          }),
          "isNamedType" -> v(field.fieldType.isInstanceOf[NamedType]),
          "passthroughFields" -> {
            val insides = buildPassthroughFields(field.fieldType)
            if (field.requiredness.isOptional) {
              v(Dictionary(
                "ptIter" -> insides
              ))
            } else {
              insides
            }
          },
          "isEnum" -> v(field.fieldType.isInstanceOf[EnumType]),
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
            (field.fieldType match {
              case ListType(eltType, _) => List(genType(eltType))
              case SetType(eltType, _) => List(genType(eltType))
              case MapType(keyType, valueType, _) => List(
                codify("(" + genType(keyType).toData + ", " + genType(valueType).toData + ")"))
              case _ => Nil
            }) map { t => Dictionary("elementType" -> t) }
          },
          "readFieldValueName" -> genID(field.sid.toTitleCase.prepend("read").append("Value")),
          "writeFieldName" -> genID(field.sid.toTitleCase.prepend("write").append("Field")),
          "writeFieldValueName" -> genID(field.sid.toTitleCase.prepend("write").append("Value")),
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
          "withoutPassthrough" -> v(templates("withoutPassthrough")),
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

  val basePassthrough = Dictionary(
    "ptStruct" -> v(false),
    "ptIter" -> v(false),
    "ptMap" -> v(false),
    "ptPrimitive" -> v(false)
  )
  private def buildPassthroughFields(fieldType: FieldType): Value = {
    val overrides =
      fieldType match {
        case _: StructType => Dictionary("ptStruct" ->
          v(Dictionary(
            "className" -> genType(fieldType)
          ))
        )
        case t: SetType => Dictionary("ptIter" ->
          buildPassthroughFields(t.eltType)
        )
        case t: ListType => Dictionary("ptIter" ->
          buildPassthroughFields(t.eltType)
        )
        case t: MapType => Dictionary("ptMap" ->
          v(Dictionary(
            "ptKey" -> buildPassthroughFields(t.keyType),
            "ptValue" -> buildPassthroughFields(t.valueType)
          ))
        )
        case _ => Dictionary("ptPrimitive" -> v(true))
      }

    v(basePassthrough + overrides)
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
    val isStruct = struct.isInstanceOf[Struct]
    val isException = struct.isInstanceOf[Exception_]
    val isUnion = struct.isInstanceOf[Union]
    val parentType =
      if (isException) {
        if (serviceOptions contains WithFinagle) {
          "ThriftException with com.twitter.finagle.SourcedException with ThriftStruct"
        } else {
          "ThriftException with ThriftStruct"
        }
      } else if (isUnion) {
        "ThriftUnion with ThriftStruct"
      } else {
        "ThriftStruct"
      }
    val arity = struct.fields.size
    val product = if (arity >= 1 && arity <= 22) {
      val fieldTypes = struct.fields.map {
        f => genFieldType(f).toData
      }.mkString(", ")
      "scala.Product" + arity + "[" + fieldTypes + "]"
    } else {
      "scala.Product"
    }

    val exceptionMsgField: Option[SimpleID] = if (isException) exceptionMsgFieldName(struct) else None

    val fieldDictionaries = fieldsToDict(
      struct.fields,
      if (isException) Seq("message") else Seq())

    val isPublic = namespace.isDefined
    val structName = if (isPublic) genID(struct.sid.toTitleCase) else genID(struct.sid)

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
        structName,
      "InstanceClassName" -> (if (isStruct) codify("Immutable") else structName),
      "underlyingStructName" -> genID(struct.sid.prepend("_underlying_")),
      "arity" -> codify(arity.toString),
      "isException" -> v(isException),
      "hasExceptionMessage" -> v(exceptionMsgField.isDefined),
      "exceptionMessageField" -> exceptionMsgField.map { genID(_) }.getOrElse { codify("")},
      "product" -> codify(product),
      "arity0" -> v(arity == 0),
      "arity1" -> v((if (arity == 1) fieldDictionaries.take(1) else Nil)),
      "arityN" -> v(arity > 1 && arity <= 22),
      "withFieldGettersAndSetters" -> v(isStruct || isException),
      "withTrait" -> v(isStruct),
      "structAnnotations" -> v(StructTemplate.renderPairs(struct.annotations))
    )
  }
}

object StructTemplate {
  /**
   * Renders a map as:
   *   Dictionary("pairs" -> ListValue(Seq(Dictionary("key" -> ..., "value" -> ...)))
   */
  def renderPairs(pairs: Map[String, String]): Dictionary = {
    val pairDicts: Seq[Dictionary] = (pairs.map { kv =>
      Dictionary("key" -> codify(kv._1), "value" -> codify(kv._2))
    } toSeq)
    Dictionary("pairs" -> v(pairDicts))
  }
}

