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
    case _: EnumType => v("I32")
    case _ => genConstType(t)
  }

  def readWriteInfo[T <: FieldType](sid: SimpleID, t: FieldType): Dictionary = {
    t match {
      case t: ListType =>
        val elt = sid.append("_element")
        TypeTemplate + Dictionary(
          "fieldType" -> genType(t),
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
          "fieldType" -> genType(t),
          "isSet" -> v(Dictionary(
            "name" -> genID(sid),
            "eltName" -> genID(elt),
            "eltConstType" -> genConstType(t.eltType),
            "eltWireConstType" -> genWireConstType(t.eltType),
            "eltType" -> genType(t.eltType),
            "isEnumSet" -> v(t.eltType.isInstanceOf[EnumType]),
            "eltReadWriteInfo" -> v(readWriteInfo(elt, t.eltType))
          )))
      case t: MapType =>
        val key =  sid.append("_key")
        val value =  sid.append("_value")
        TypeTemplate + Dictionary(
          "fieldType" -> genType(t),
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
          "isNamedType" -> v(true),
          "isImported" -> v(t.scopePrefix.isDefined),
          "fieldType" -> genType(t),
          "isStruct" -> v(Dictionary(
            "name" -> genID(sid)
          )))
      case t: EnumType =>
        TypeTemplate + Dictionary(
          "isNamedType" -> v(true),
          "isImported" -> v(t.scopePrefix.isDefined),
          "fieldType" -> genType(t),
          "isEnum" -> v(Dictionary(
            "name" -> genID(sid)
          )))
      case t: BaseType =>
        TypeTemplate + Dictionary(
          "fieldType" -> genType(t),
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

  def fieldsToDict(
    fields: Seq[Field],
    blacklist: Seq[String],
    namespace: Option[Identifier] = None
  ): Seq[Dictionary] = {
    fields.zipWithIndex map {
      case (field, index) =>
        val valueVariableID = field.sid.append("_item")
        val fieldName = genID(field.sid)
        val camelCaseFieldName = if (fieldName.toString.indexOf('_') >= 0)
          genID(field.sid.toCamelCase)
        else
          NoValue

        Dictionary(
          "index" -> v(index.toString),
          "indexP1" -> v((index + 1).toString),
          "_fieldName" -> genID(field.sid.prepend("_")), // for Java only
          "unsetName" -> genID(field.sid.toTitleCase.prepend("unset")),
          "readName" -> genID(field.sid.toTitleCase.prepend("read")),
          "getBlobName" -> genID(field.sid.toTitleCase.prepend("get").append("Blob")),
          "readBlobName" -> genID(field.sid.toTitleCase.prepend("read").append("Blob")),
          "getName" -> genID(field.sid.toTitleCase.prepend("get")), // for Java only
          "isSetName" -> genID(field.sid.toTitleCase.prepend("isSet")), // for Java only
          "fieldName" -> fieldName,
          "fieldNameForWire" -> v(field.originalName),
          "fieldNameCamelCase" -> camelCaseFieldName,
          "newFieldName" -> genID(field.sid.toTitleCase.prepend("new")),
          "FieldName" -> genID(field.sid.toTitleCase),
          "FIELD_NAME" -> genID(field.sid.toUpperCase),
          "gotName" -> genID(field.sid.prepend("_got_")),
          "id" -> v(field.index.toString),
          "fieldConst" -> genID(field.sid.toTitleCase.append("Field")),
          "constType" -> genConstType(field.fieldType),
          "isPrimitive" -> v(isPrimitive(field.fieldType)),
          "isLazyReadEnabled" -> v(isLazyReadEnabled(field.fieldType, field.requiredness.isOptional)),
          "primitiveFieldType" -> genPrimitiveType(field.fieldType),
          "fieldType" -> genType(field.fieldType),
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
          "fieldTypeAnnotations" -> StructTemplate.renderPairs(field.typeAnnotations),
          "fieldFieldAnnotations" -> StructTemplate.renderPairs(field.fieldAnnotations),
          "isImported" -> v(field.fieldType match {
            case n: NamedType => n.scopePrefix.isDefined
            case _ => false
          }),
          "isNamedType" -> v(field.fieldType.isInstanceOf[NamedType]),
          "passthroughFields" -> {
            val insides = buildPassthroughFields(field.fieldType, namespace)
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
                v("(" + genType(keyType).toData + ", " + genType(valueType).toData + ")"))
              case _ => Nil
            }) map { t => Dictionary("elementType" -> t) }
          },
          "readFieldValueName" -> genID(field.sid.toTitleCase.prepend("read").append("Value")),
          "writeFieldName" -> genID(field.sid.toTitleCase.prepend("write").append("Field")),
          "writeFieldValueName" -> genID(field.sid.toTitleCase.prepend("write").append("Value")),
          "readField" -> v(templates("readField")),
          "decodeProtocol" -> genDecodeProtocolMethod(field.fieldType),
          "offsetSkipProtocol" -> genOffsetSkipProtocolMethod(field.fieldType),
          "readUnionField" -> v(templates("readUnionField")),
          "readLazyField" -> v(templates("readLazyField")),
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
  private def buildPassthroughFields(fieldType: FieldType, namespace: Option[Identifier]): Value = {
    val overrides =
      fieldType match {
        case _: StructType => Dictionary("ptStruct" ->
          v(Dictionary(
            "className" -> genType(fieldType, namespace)
          ))
        )
        case t: SetType => Dictionary("ptIter" ->
          buildPassthroughFields(t.eltType, namespace)
        )
        case t: ListType => Dictionary("ptIter" ->
          buildPassthroughFields(t.eltType, namespace)
        )
        case t: MapType => Dictionary("ptMap" ->
          v(Dictionary(
            "ptKey" -> buildPassthroughFields(t.keyType, namespace),
            "ptValue" -> buildPassthroughFields(t.valueType, namespace)
          ))
        )
        case _ => Dictionary("ptPrimitive" -> v(true))
      }

    v(basePassthrough + overrides)
  }

  private def exceptionMsgFieldName(struct: StructLike): Option[SimpleID] = {
    val msgField: Option[Field] = struct.fields.find { field =>
      // 1st choice: find a field called message
      field.sid.name == "message"
    }.orElse {
      // 2nd choice: the first string field
      struct.fields.find {
        field => field.fieldType == TString
      }
    }

    msgField.map { _.sid }
  }

  def getSuccessType(result: FunctionResult, namespace: Option[Identifier]): CodeFragment =
    result.success match {
      case Some(field) => genType(field.fieldType, namespace)
      case None => v("Unit")
    }

  def getSuccessValue(result: FunctionResult): CodeFragment =
    result.success match {
      case Some(field) => v("success")
      case None => v("Some(Unit)")
    }

  def getExceptionFields(result: FunctionResult): CodeFragment = {
    val exceptions = result.exceptions.map { field: Field => genID(field.sid).toData }.mkString(", ")
    v(s"Seq($exceptions)")
  }

  def structDict(
    struct: StructLike,
    namespace: Option[Identifier],
    includes: Seq[Include],
    serviceOptions: Set[ServiceOption],
    toplevel: Boolean = false // True if this struct is defined in its own file. False for internal structs.
  ): Dictionary = {
    val parentType = struct match {
      case e: Exception_ if (serviceOptions contains WithFinagle) =>
        "ThriftException with com.twitter.finagle.SourcedException with ThriftStruct"
      case e: Exception_ => "ThriftException with ThriftStruct"
      case u: Union => "ThriftUnion with ThriftStruct"
      case result: FunctionResult =>
        val resultType = getSuccessType(result, namespace)
        s"ThriftResponse[$resultType] with ThriftStruct"
      case _ => "ThriftStruct"
    }
    val arity = struct.fields.size

    val isStruct = struct.isInstanceOf[Struct]
    val isException = struct.isInstanceOf[Exception_]
    val isUnion = struct.isInstanceOf[Union]
    val isResponse = struct.isInstanceOf[FunctionResult]

    val exceptionMsgField: Option[SimpleID] =
      if (isException) exceptionMsgFieldName(struct) else None

    val fieldDictionaries = fieldsToDict(
      struct.fields,
      if (isException) Seq("message") else Nil,
      namespace
    )

    val structName = if (toplevel) genID(struct.sid.toTitleCase) else genID(struct.sid)

    Dictionary(
      "public" -> v(toplevel),
      "package" -> namespace.map(genID).getOrElse(v("")),
      "docstring" -> v(struct.docstring.getOrElse("")),
      "parentType" -> v(parentType),
      "fields" -> v(fieldDictionaries),
      "defaultFields" -> v(fieldsToDict(struct.fields.filter(!_.requiredness.isOptional), Nil)),
      "alternativeConstructor" -> v(
        struct.fields.exists(_.requiredness.isOptional)
        && struct.fields.exists(_.requiredness.isDefault)),
      "StructNameForWire" -> v(struct.originalName),
      "StructName" ->
        structName,
      "InstanceClassName" -> (if (isStruct) v("Immutable") else structName),
      "underlyingStructName" -> genID(struct.sid.prepend("_underlying_")),
      "arity" -> v(arity.toString),
      "isException" -> v(isException),
      "isResponse" -> v(isResponse),
      "hasExceptionMessage" -> v(exceptionMsgField.isDefined),
      "exceptionMessageField" -> exceptionMsgField.map(genID).getOrElse { v("")},
      "product" -> v(productN(struct.fields, namespace)),
      "arity0" -> v(arity == 0),
      "arity1" -> v((if (arity == 1) fieldDictionaries.take(1) else Nil)),
      "arityN" -> v(arity > 1 && arity <= 22),
      "withFieldGettersAndSetters" -> v(isStruct || isException),
      "withTrait" -> v(isStruct),
      "structAnnotations" -> StructTemplate.renderPairs(struct.annotations)
    )
  }
}

object StructTemplate {

  /**
   * Renders a map as:
   *   Dictionary("pairs" -> ListValue(Seq(Dictionary("key" -> ..., "value" -> ...)))
   */
  private def renderPairs(pairs: Map[String, String]): Dictionary.Value = {
    if (pairs.isEmpty) {
      NoValue
    } else {
      val pairDicts: Seq[Dictionary] =
        pairs.map { case (key, value) => Dictionary("key" -> v(key), "value" -> v(value)) }.toSeq
      v(Dictionary("pairs" -> v(pairDicts)))
    }
  }
}
