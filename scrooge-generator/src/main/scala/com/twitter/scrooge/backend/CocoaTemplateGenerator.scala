package com.twitter.scrooge.backend

import com.twitter.scrooge.ast._
import com.twitter.scrooge.mustache.Dictionary

trait CocoaTemplateGenerator extends TemplateGenerator {
  implicit class RichDictionary(dictionary: Dictionary) {
    def update(keyPath: Seq[String], data: String) : Unit = {
      keyPath match {
        case head :: Nil => dictionary(head) = data
        case head :: tail => dictionary(head).children.head(tail) = data
        case _ =>
      }
    }

    def update(keyPath: Seq[String], data: Boolean) {
      keyPath match {
        case head :: Nil => dictionary(head) = data
        case head :: tail => dictionary(head).children.head(tail) = data
        case _ =>
      }
    }
  }

  def getFieldNSCoderMethod(f: Field, isDecode: Boolean = false): String = {
    val prefix = if (isDecode) "decode" else "encode"
    val suffix = if (isDecode) "ForKey" else ""
    val code = f.fieldType match {
      case TBool => "Bool"
      case TByte => "Int32"
      case TI16 => "Int32"
      case TI32 => "Int32"
      case TI64 => "Int64"
      case TDouble => "Double"
      case TBinary => "DataObject"
      case EnumType(_, _) => "Int32"
      case _ => "Object"
    }
    prefix + code + suffix
  }

  def getTypeValueMethod(t: FieldType, name: String): String = {
    val code = t match {
      case TBool => "[%s boolValue]"
      case TByte => "[%s intValue]"
      case TI16 => "[%s intValue]"
      case TI32 => "[%s intValue]"
      case TI64 => "[%s longLongValue]"
      case TDouble => "[%s doubleValue]"
      case EnumType(_, _) => "[%s intValue]"
      case _ => "%s"
    }
    code format name
  }

  def getDependentTypes(struct: StructLike): Set[FieldType] = {
    def getDependentTypes(fieldType: FieldType): Set[FieldType] = {
      fieldType match {
        case t: ListType => getDependentTypes(t.eltType)
        case t: MapType => getDependentTypes(t.keyType) ++ getDependentTypes(t.valueType)
        case t: SetType => getDependentTypes(t.eltType)
        case StructType(_, _) => Set(fieldType)
        case EnumType(_, _) => Set(fieldType)
        case _ => Set()
      }
    }

    struct.fields.map(field => getDependentTypes(field.fieldType)).reduceLeft(_ ++ _)
  }

  def getDependentHeaders(struct: StructLike): String = {
    getDependentTypes(struct).map(t => s"""#import \"${genType(t).toString}.h\"""").toList.sorted.mkString("\n")
  }

  override def readWriteInfo[T <: FieldType](sid: SimpleID, t: FieldType): Dictionary = {
    val dictionary = super.readWriteInfo(sid, t)
    t match {
      case t: MapType => {
        dictionary(Seq("isMap", "isKeyPrimitive")) = isPrimitive(t.keyType) || t.keyType.isInstanceOf[EnumType]
        dictionary(Seq("isMap", "keyGetValueMethod")) = getTypeValueMethod(t.keyType, genID(sid) + "_key_id")
        dictionary(Seq("isMap", "valueGetValueMethod")) = getTypeValueMethod(t.valueType, genID(sid) + "_value_id")
      }
      case _ =>
    }
    dictionary
  }

  override def fieldsToDict(fields: Seq[Field], blacklist: Seq[String]) = {
    val dictionaries = super.fieldsToDict(fields, blacklist)

    (dictionaries, fields, 0 until dictionaries.size).zipped.foreach {
      case (dictionary, field, index) =>
        dictionary("decodeMethod") = getFieldNSCoderMethod(field, true)
        dictionary("encodeMethod") = getFieldNSCoderMethod(field, false)
        dictionary("fieldNameCamelCase") = genID(field.sid.toCamelCase).toString
        dictionary("fieldNameInInit") = genID(if (index == 0) field.sid.toTitleCase else field.sid.toCamelCase).toString
        dictionary("isPrimitive") = isPrimitive(field.fieldType) || field.fieldType.isInstanceOf[EnumType]
        dictionary("wireConstType") = genWireConstType(field.fieldType).data
    }

    dictionaries
  }

  override def structDict(struct: StructLike, namespace: Option[Identifier], includes: Seq[Include], serviceOptions: Set[ServiceOption]) = {
    val dictionary = super.structDict(struct, namespace, includes, serviceOptions)
    dictionary("headers") = getDependentHeaders(struct)

    dictionary
  }
}

