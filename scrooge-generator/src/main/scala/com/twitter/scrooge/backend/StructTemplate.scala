package com.twitter.scrooge.backend

/**
 * Copyright 2011 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
  import Generator._

  case class Binding[FT <: FieldType](name: String, fieldType: FT)

  val TypeTemplate: Dictionary =
    Dictionary(
      "isList" -> v(false),
      "isSet" -> v(false),
      "isMap" -> v(false),
      "isStruct" -> v(false),
      "isEnum" -> v(false),
      "isBase" -> v(false),
      "isCollection" -> v(false)
    )

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
          "isCollection" -> v(true),
          "isList" -> v(
            Dictionary(
              "name" -> genID(sid),
              "eltName" -> genID(elt),
              "eltConstType" -> genConstType(t.eltType),
              "eltWireConstType" -> genWireConstType(t.eltType),
              "eltType" -> genType(t.eltType),
              "eltReadWriteInfo" -> v(readWriteInfo(elt, t.eltType))
            )
          )
        )
      case t: SetType =>
        val elt = sid.append("_element")
        TypeTemplate + Dictionary(
          "fieldType" -> genType(t),
          "isCollection" -> v(true),
          "isSet" -> v(
            Dictionary(
              "name" -> genID(sid),
              "eltName" -> genID(elt),
              "eltConstType" -> genConstType(t.eltType),
              "eltWireConstType" -> genWireConstType(t.eltType),
              "eltType" -> genType(t.eltType),
              "isEnumSet" -> v(t.eltType.isInstanceOf[EnumType]),
              "eltReadWriteInfo" -> v(readWriteInfo(elt, t.eltType))
            )
          )
        )
      case t: MapType =>
        val key = sid.append("_key")
        val value = sid.append("_value")
        TypeTemplate + Dictionary(
          "fieldType" -> genType(t),
          "isCollection" -> v(true),
          "isMap" -> v(
            Dictionary(
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
            )
          )
        )
      case t: StructType =>
        TypeTemplate + Dictionary(
          "isNamedType" -> v(true),
          "isImported" -> v(t.scopePrefix.isDefined),
          "fieldType" -> genType(t),
          "isStruct" -> v(
            Dictionary(
              "name" -> genID(sid)
            )
          )
        )
      case t: EnumType =>
        TypeTemplate + Dictionary(
          "isNamedType" -> v(true),
          "isImported" -> v(t.scopePrefix.isDefined),
          "fieldType" -> {
            genType(t.copy(enum = t.enum.copy(t.enum.sid.toTitleCase)))
          },
          "isEnum" -> v(
            Dictionary(
              "name" -> genID(sid)
            )
          )
        )
      case t: BaseType =>
        TypeTemplate + Dictionary(
          "fieldType" -> genType(t),
          "isBase" -> v(
            Dictionary(
              "type" -> genType(t),
              "name" -> genID(sid),
              "protocolWriteMethod" -> genProtocolWriteMethod(t),
              "protocolReadMethod" -> genProtocolReadMethod(t),
              "protocolSkipMethod" -> genProtocolSkipMethod(t)
            )
          )
        )
      case _: ReferenceType =>
        throw new ScroogeInternalException("ReferenceType should have been resolved by now")
    }
  }

  /**
   * Generates code that reads the fieldType from a TProtocol variable of name `protoName`
   * and a TProtocols instance named `_protos`.
   *
   * For an i32: "readI32($protoName)"
   *
   * For a list<string>:
   * "readList($protoName, TProtocols.readStringFn)
   *
   * For a set<list<i32>>:
   * "readSet($protoName, proto => { readList(proto, TProtocols.readI32Fn) })"
   */
  private[this] def genReadValue(fieldType: FieldType, protoName: String): CodeFragment = {
    fieldType match {
      case TBool => v(s"$protoName.readBool()")
      case TByte => v(s"$protoName.readByte()")
      case TI16 => v(s"$protoName.readI16()")
      case TI32 => v(s"$protoName.readI32()")
      case TI64 => v(s"$protoName.readI64()")
      case TDouble => v(s"$protoName.readDouble()")
      case TString => v(s"$protoName.readString()")
      case TBinary => v(s"$protoName.readBinary()")
      case _: StructType => genType(fieldType).append(s".decode($protoName)")
      case _: EnumType => genType(fieldType).append(s".getOrUnknown($protoName.readI32())")
      case t: SetType =>
        val readElement = genReadValueFn1(t.eltType)
        v(s"_protos.readSet($protoName, $readElement)")
      case t: ListType =>
        val readElement = genReadValueFn1(t.eltType)
        v(s"_protos.readList($protoName, $readElement)")
      case t: MapType =>
        val readKey = genReadValueFn1(t.keyType)
        val readVal = genReadValueFn1(t.valueType)
        v(s"_protos.readMap($protoName, $readKey, $readVal)")
      case _ => throw new IllegalArgumentException(s"Unsupported FieldType: $fieldType")
    }
  }

  /**
   * Similar to [[genReadValue]]. However, for collections it will delegate
   * to a generated function for reading the value.
   *
   * @note This is done to share code across struct's lazyDecode and eagerDecode
   *       implementations.
   */
  private[this] def genReadValueOrMethod(
    simpleID: SimpleID,
    fieldType: FieldType,
    protoName: String
  ): CodeFragment = {
    fieldType match {
      case _: ContainerType =>
        genID(simpleID.toTitleCase.prepend("read").append(s"Value($protoName)"))
      case _ =>
        genReadValue(fieldType, protoName)
    }
  }

  /**
   * Generates code that is a `Function1[TProtocol, T]`.
   * This is code that typically be used by `TProtocols` to read a single
   * instance of a `Field`.
   *
   * For an i32:
   * "TProtocols.readI32Fn"
   *
   * For a list<string>:
   * "proto => { readList(proto, TProtocols.readStringFn }"
   *
   * For a set<list<i32>>:
   * "proto => { readSet(proto => { readList(protocol, TProtocols.readStringFn) }}}"
   */
  private[this] def genReadValueFn1(fieldType: FieldType): CodeFragment = {
    fieldType match {
      case TBool => v("_root_.com.twitter.scrooge.internal.TProtocols.readBoolFn")
      case TByte => v("_root_.com.twitter.scrooge.internal.TProtocols.readByteFn")
      case TI16 => v("_root_.com.twitter.scrooge.internal.TProtocols.readI16Fn")
      case TI32 => v("_root_.com.twitter.scrooge.internal.TProtocols.readI32Fn")
      case TI64 => v("_root_.com.twitter.scrooge.internal.TProtocols.readI64Fn")
      case TDouble => v("_root_.com.twitter.scrooge.internal.TProtocols.readDoubleFn")
      case TString => v("_root_.com.twitter.scrooge.internal.TProtocols.readStringFn")
      case TBinary => v("_root_.com.twitter.scrooge.internal.TProtocols.readBinaryFn")
      case _: StructType => genType(fieldType).append(s".decode _")
      case _ =>
        val readElem = genReadValue(fieldType, "proto")
        v(s"proto => { $readElem }")
    }
  }

  private[this] def genWriteValueFn2(fieldType: FieldType): CodeFragment = {
    fieldType match {
      case TBool =>
        v("_root_.com.twitter.scrooge.internal.TProtocols.writeBoolFn")
      case TByte =>
        v("_root_.com.twitter.scrooge.internal.TProtocols.writeByteFn")
      case TI16 =>
        v("_root_.com.twitter.scrooge.internal.TProtocols.writeI16Fn")
      case TI32 =>
        v("_root_.com.twitter.scrooge.internal.TProtocols.writeI32Fn")
      case TI64 =>
        v("_root_.com.twitter.scrooge.internal.TProtocols.writeI64Fn")
      case TDouble =>
        v("_root_.com.twitter.scrooge.internal.TProtocols.writeDoubleFn")
      case TString =>
        v("_root_.com.twitter.scrooge.internal.TProtocols.writeStringFn")
      case TBinary =>
        v("_root_.com.twitter.scrooge.internal.TProtocols.writeBinaryFn")
      case _: EnumType =>
        v("_root_.com.twitter.scrooge.internal.TProtocols.writeEnumFn")
      case _: StructType =>
        val structType = genType(fieldType)
        v(s"(proto, elem: $structType) => elem.write(proto)")
      case _: ContainerType =>
        val fieldName = v("elem")
        val elemType = fieldType match {
          case _: EnumType => v("Int")
          case _ => genType(fieldType)
        }
        val writeElem = genWriteValue(fieldName, fieldType, "proto")
        v(s"(proto, elem: $elemType) => { $writeElem }")
      case _ => throw new IllegalArgumentException(s"Unsupported FieldType: $fieldType")
    }
  }

  private[this] def genWriteValue(
    fieldName: CodeFragment,
    fieldType: FieldType,
    protoName: String
  ): CodeFragment = {
    fieldType match {
      case TBool =>
        v(s"$protoName.writeBool($fieldName)")
      case TByte =>
        v(s"$protoName.writeByte($fieldName)")
      case TI16 =>
        v(s"$protoName.writeI16($fieldName)")
      case TI32 =>
        v(s"$protoName.writeI32($fieldName)")
      case TI64 =>
        v(s"$protoName.writeI64($fieldName)")
      case TDouble =>
        v(s"$protoName.writeDouble($fieldName)")
      case TString =>
        v(s"$protoName.writeString($fieldName)")
      case TBinary =>
        v(s"$protoName.writeBinary($fieldName)")
      case _: EnumType =>
        v(s"$protoName.writeI32($fieldName.value)")
      case _: StructType =>
        v(s"$fieldName.write($protoName)")
      case t: SetType =>
        val elemFieldType = s"TType.${genConstType(t.eltType)}"
        val writeElement = genWriteValueFn2(t.eltType)
        v(s"_protos.writeSet($protoName, $fieldName, $elemFieldType, $writeElement)")
      case t: ListType =>
        val elemFieldType = s"TType.${genConstType(t.eltType)}"
        val writeElement = genWriteValueFn2(t.eltType)
        v(s"_protos.writeList($protoName, $fieldName, $elemFieldType, $writeElement)")
      case t: MapType =>
        val keyType = s"TType.${genConstType(t.keyType)}"
        val valType = s"TType.${genConstType(t.valueType)}"
        val writeKey = genWriteValueFn2(t.keyType)
        val writeVal = genWriteValueFn2(t.valueType)
        v(s"_protos.writeMap($protoName, $fieldName, $keyType, $writeKey, $valType, $writeVal)")
      case _ => throw new IllegalArgumentException(s"Unsupported FieldType: $fieldType")
    }
  }

  private[this] def genWriteValueOrMethod(
    fieldName: CodeFragment,
    fieldType: FieldType,
    isOptional: Boolean,
    protoName: String
  ): CodeFragment = {
    val dereferencedName =
      if (isOptional) fieldName.append(".get") else fieldName
    fieldType match {
      case _: ContainerType =>
        val titleCase = Identifier.toTitleCase(fieldName.toData)
        v(s"write${titleCase}Value($protoName, $dereferencedName)")
      case _ =>
        genWriteValue(dereferencedName, fieldType, protoName)
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
        val camelCaseFieldName =
          if (fieldName.toString.indexOf('_') >= 0)
            genID(field.sid.toCamelCase)
          else
            NoValue

        Dictionary(
          "index" -> v(index.toString),
          "indexP1" -> v((index + 1).toString),
          "_fieldName" -> genID(field.sid.prepend("_")), // for Java and Scala only
          "unsetName" -> genID(field.sid.toTitleCase.prepend("unset")),
          "readName" -> genID(field.sid.toTitleCase.prepend("read")),
          "getBlobName" -> genID(field.sid.toTitleCase.prepend("get").append("Blob")),
          "readBlobName" -> genID(field.sid.toTitleCase.prepend("read").append("Blob")),
          "getName" -> genID(field.sid.toTitleCase.prepend("get")), // for Java only
          "isSetName" -> genID(field.sid.toTitleCase.prepend("isSet")), // for Java only
          "fieldName" -> fieldName,
          "docstring" -> v(field.docstring.getOrElse("")),
          "fieldNameForWire" -> v(field.originalName),
          "fieldNameCamelCase" -> camelCaseFieldName,
          "setName" -> genID(field.sid.toCamelCase.prepend("set_")), // for Scala only
          "delegateName" -> genID(field.sid.toCamelCase.prepend("delegated_")), // for Scala only
          "memberName" -> genID(field.sid.toCamelCase.prepend("m_")), // for Scala only
          "newFieldName" -> genID(field.sid.toTitleCase.prepend("new")),
          "FieldName" -> genID(field.sid.toTitleCase),
          "FIELD_NAME" -> genID(field.sid.toUpperCase),
          "gotName" -> genID(field.sid.prepend("_got_")),
          "id" -> v(field.index.toString),
          "fieldConst" -> genID(field.sid.toTitleCase.append("Field")),
          "constType" -> genConstType(field.fieldType),
          "isPrimitive" -> v(isPrimitive(field.fieldType)),
          "isLazyReadEnabled" -> v(
            isLazyReadEnabled(field.fieldType, field.requiredness.isOptional)
          ),
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
          "fieldTypeAnnotations" -> TemplateGenerator.renderPairs(field.typeAnnotations),
          "fieldFieldAnnotations" -> TemplateGenerator.renderPairs(field.fieldAnnotations),
          "isImported" -> v(field.fieldType match {
            case n: NamedType => n.scopePrefix.isDefined
            case _ => false
          }),
          "isNamedType" -> v(field.fieldType.isInstanceOf[NamedType]),
          "hasPassthroughFields" -> v(!canCallWithoutPassthroughFields(field.fieldType)),
          "passthroughFields" -> {
            val insides = buildPassthroughFields(field.fieldType)
            if (field.requiredness.isOptional) {
              v(
                Dictionary(
                  "ptIter" -> insides
                )
              )
            } else {
              insides
            }
          },
          "isEnum" -> v(field.fieldType.isInstanceOf[EnumType]),
          "isStruct" -> v(field.fieldType.isInstanceOf[StructType]),
          // "qualifiedFieldType" is used to generate qualified type name even if it's not
          // imported, in case other same-named entities are generated in the same file.
          "qualifiedFieldType" -> v(templates("qualifiedFieldType")),
          "hasDefaultValue" -> v(genDefaultFieldValue(field).isDefined),
          "defaultFieldValue" -> genDefaultFieldValue(field).getOrElse(NoValue),
          // these two alternate default values are for constructors that do not accept Options for
          // construction required fields.
          "hasAlternateDefaultValue" -> v(
            genDefaultFieldValue(field, forAlternateConstructor = true).isDefined
          ),
          "alternateDefaultFieldValue" -> genDefaultFieldValue(
            field,
            forAlternateConstructor = true
          ).getOrElse(NoValue),
          "hasDefaultFieldValueForFieldInfo" -> v(
            genDefaultFieldValueForFieldInfo(field).isDefined
          ),
          "defaultFieldValueForFieldInfo" -> genDefaultFieldValueForFieldInfo(field)
            .getOrElse(NoValue),
          "defaultReadValue" -> genDefaultReadValue(field),
          "unsafeEmptyReadValue" -> genUnsafeEmptyReadValue(field),
          "hasGetter" -> v(!blacklist.contains(field.sid.name)),
          "hasIsDefined" -> v(
            field.requiredness.isOptional || (!field.requiredness.isRequired && !isPrimitive(
              field.fieldType
            ))
          ),
          "required" -> v(field.requiredness.isRequired),
          "optional" -> v(field.requiredness.isOptional),
          "nullable" -> v(isNullableType(field.fieldType, field.requiredness.isOptional)),
          "constructionOptional" -> v(
            !isConstructionRequiredField(field) && field.requiredness.isOptional
          ),
          "constructionRequired" -> v(
            isConstructionRequiredField(field)
          ),
          "collection" -> v {
            (field.fieldType match {
              case ListType(eltType, _) => List(genType(eltType))
              case SetType(eltType, _) => List(genType(eltType))
              case MapType(keyType, valueType, _) =>
                List(v("(" + genType(keyType).toData + ", " + genType(valueType).toData + ")"))
              case _ => Nil
            }) map { t => Dictionary("elementType" -> t) }
          },
          "readFieldValue" -> genReadValue(field.fieldType, "_iprot"),
          "readFieldValueOrMethod" -> genReadValueOrMethod(field.sid, field.fieldType, "_iprot"),
          "readFieldValueName" -> genID(field.sid.toTitleCase.prepend("read").append("Value")),
          "writeFieldName" -> genID(field.sid.toTitleCase.prepend("write").append("Field")),
          "writeFieldValueName" -> genID(field.sid.toTitleCase.prepend("write").append("Value")),
          "writeFieldValue" -> genWriteValue(v("_value"), field.fieldType, "_oprot"),
          "writeValueOrMethod" -> genWriteValueOrMethod(
            genID(field.sid),
            field.fieldType,
            field.requiredness.isOptional,
            "_oprot"),
          "readField" -> v(templates("readField")),
          "decodeProtocol" -> genDecodeProtocolMethod(field.fieldType),
          "offsetSkipProtocol" -> genOffsetSkipProtocolMethod(field.fieldType),
          "readUnionField" -> v(templates("readUnionField")),
          "readLazyField" -> v(templates("readLazyField")),
          "readAdaptField" -> v(templates("readAdaptField")),
          "readValue" -> v(templates("readValue")),
          "skipValue" -> v(templates("skipValue")),
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
          "constructionOptionalType" -> v(templates("constructionOptionalType")),
          "withoutPassthrough" -> v(templates("withoutPassthrough")),
          "readWriteInfo" -> v(readWriteInfo(valueVariableID, field.fieldType)),
          "valueVariableName" -> genID(valueVariableID),
          "first" -> v(fields.head.index == field.index),
          "last" -> v(fields.last.index == field.index)
        )
    }
  }

  val basePassthrough: Dictionary = Dictionary(
    "ptStruct" -> v(false),
    "ptIter" -> v(false),
    "ptMap" -> v(false),
    "ptPrimitive" -> v(false)
  )
  private def buildPassthroughFields(fieldType: FieldType): Value = {
    val overrides =
      fieldType match {
        case _: StructType =>
          Dictionary(
            "ptStruct" ->
              v(
                Dictionary(
                  "className" -> genType(fieldType)
                )
              )
          )
        case t: SetType =>
          Dictionary(
            "ptIter" ->
              buildPassthroughFields(t.eltType)
          )
        case t: ListType =>
          Dictionary(
            "ptIter" ->
              buildPassthroughFields(t.eltType)
          )
        case t: MapType =>
          Dictionary(
            "ptMap" ->
              v(
                Dictionary(
                  "ptKey" -> buildPassthroughFields(t.keyType),
                  "ptValue" -> buildPassthroughFields(t.valueType)
                )
              )
          )
        case _ => Dictionary("ptPrimitive" -> v(true))
      }

    v(basePassthrough + overrides)
  }

  /**
   * Returns whether the fieldType is such that it cannot possibly contain passthrough fields.
   *
   * Examples - primitive types, Strings do not contain pass through.
   * Collections containing only primitive elements also cannot.
   *
   * @param fieldType Field type being checked
   * @return
   */
  private def canCallWithoutPassthroughFields(fieldType: FieldType): Boolean = {
    fieldType match {
      case t if isPrimitive(t) =>
        true
      case TBinary | TString =>
        true
      case _: EnumType =>
        true
      case ListType(eltType, _) =>
        canCallWithoutPassthroughFields(eltType)
      case SetType(eltType, _) =>
        canCallWithoutPassthroughFields(eltType)
      case MapType(keyType, valueType, _) =>
        canCallWithoutPassthroughFields(keyType) && canCallWithoutPassthroughFields(valueType)
      case _ =>
        false
    }
  }

  private def exceptionMsgFieldName(struct: StructLike): Option[SimpleID] = {
    val msgField: Option[Field] = struct.fields
      .find { field =>
        // 1st choice: find a field called message
        field.sid.name == "message"
      }
      .orElse {
        // 2nd choice: the first string field
        struct.fields.find { field => field.fieldType == TString }
      }

    msgField.map { _.sid }
  }

  def getSuccessType(result: FunctionResult): CodeFragment =
    result.success match {
      case Some(field) => genType(field.fieldType)
      case None => v("Unit")
    }

  def getSuccessValue(result: FunctionResult): CodeFragment =
    result.success match {
      case Some(field) => v("success")
      case None => v("Some(())")
    }

  def getExceptionFields(result: FunctionResult): CodeFragment = {
    if (result.exceptions.isEmpty) {
      v("Nil")
    } else {
      val exceptions = result.exceptions
        .map { field: Field => genID(field.sid).toData }
        .mkString(", ")
      v(s"Seq($exceptions)")
    }
  }

  private def basename(fqdn: String): String = fqdn.split('.').last

  def structDict(
    struct: StructLike,
    namespace: Option[Identifier],
    includes: Seq[Include],
    serviceOptions: Set[ServiceOption],
    genAdapt: Boolean,
    toplevel: Boolean =
      false // True if this struct is defined in its own file. False for internal structs.
  ): Dictionary = {
    val fullyQualifiedThriftExn = "_root_.com.twitter.scrooge.ThriftException"
    val fullyQualifiedSourcedExn = "_root_.com.twitter.finagle.SourcedException"
    val parentType = struct match {
      case e: Exception_ if serviceOptions.contains(WithFinagle) =>
        s"$fullyQualifiedThriftExn with $fullyQualifiedSourcedExn with ThriftStruct"
      case e: Exception_ => s"$fullyQualifiedThriftExn with ThriftStruct"
      case u: Union => "ThriftUnion\n  with ThriftStruct"
      case result: FunctionResult =>
        val resultType = getSuccessType(result)
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

    val nonOptionalFields = struct.fields.filter(field =>
      field.requiredness.isRequired || field.requiredness.isDefault || Generator
        .isConstructionRequiredField(field))
    val nonOptionalFieldDictionaries = fieldsToDict(
      nonOptionalFields,
      if (isException) Seq("message") else Nil,
      namespace
    )

    val fieldDictionaries = fieldsToDict(
      struct.fields,
      if (isException) Seq("message") else Nil,
      namespace
    )
    val firstFieldName =
      if (struct.fields.nonEmpty) genID(struct.fields.head.sid.toTitleCase) else v("")
    val firstFieldValue =
      if (struct.fields.nonEmpty) genUnsafeEmptyReadValue(struct.fields.head) else v("null")

    val structName = if (toplevel) genID(struct.sid.toTitleCase) else genID(struct.sid)

    val pkg = namespace.map(genID).getOrElse(v(""))
    val pkgName = v(basename(pkg.toData))

    Dictionary(
      "public" -> v(toplevel),
      "package" -> pkg,
      "packageName" -> pkgName,
      "docstring" -> v(struct.docstring.getOrElse("")),
      "parentType" -> v(parentType),
      "firstFieldName" -> firstFieldName,
      "firstFieldValue" -> firstFieldValue,
      "fields" -> v(fieldDictionaries),
      "hasFields" -> v(fieldDictionaries.nonEmpty),
      "nonOptionalFields" -> v(nonOptionalFieldDictionaries),
      "defaultFields" -> v(fieldsToDict(struct.fields.filter(!_.requiredness.isOptional), Nil)),
      "alternativeConstructor" -> v(
        struct.fields.exists(_.requiredness.isOptional)
          && struct.fields.exists(_.requiredness.isDefault)
      ),
      "generateStructProxy" -> v(
        struct.annotations
          .getOrElse("com.twitter.scrooge.scala.generateStructProxy", "false") == "true"),
      "hasConstructionRequiredFields" -> v(
        struct.fields.exists(Generator.isConstructionRequiredField)
      ),
      "hasNonOptionalFields" -> v(nonOptionalFields.nonEmpty),
      "StructNameForWire" -> v(struct.originalName),
      "StructName" ->
        structName,
      "InstanceClassName" -> (if (isStruct) v("Immutable") else structName),
      "underlyingStructName" -> genID(struct.sid.prepend("_underlying_")),
      "arity" -> v(arity.toString),
      "isUnion" -> v(isUnion),
      "isException" -> v(isException),
      "isResponse" -> v(isResponse),
      "hasExceptionMessage" -> v(exceptionMsgField.isDefined),
      "exceptionMessageField" -> exceptionMsgField.map(genID).getOrElse { v("") },
      "product" -> v(productN(struct.fields, namespace)),
      "tuple" -> v(tupleN(struct.fields, namespace)),
      "arity0" -> v(arity == 0),
      "arity1" -> v(if (arity == 1) fieldDictionaries.take(1) else Nil),
      "arityN" -> v(arity > 1 && arity <= 22),
      "arity1ThroughN" -> v(arity >= 1 && arity <= 22),
      "withFieldGettersAndSetters" -> v(isStruct || isException),
      "withTrait" -> v(isStruct),
      "adapt" -> v(genAdapt),
      "hasFailureFlags" -> v(isException && serviceOptions.contains(WithFinagle)),
      "structAnnotations" -> TemplateGenerator.renderPairs(struct.annotations)
    )
  }
}
