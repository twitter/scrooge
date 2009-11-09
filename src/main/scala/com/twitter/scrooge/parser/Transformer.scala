package com.twitter.scrooge.parser

import scala.collection.mutable

class FieldValidationException(message: String) extends Exception(message)
class EnumValidationException(message: String) extends Exception(message)

object EnumValueTransformer extends Transformer {
  override def transformEnumValues(vs: List[EnumValue]): List[EnumValue] = {
    if (vs.exists(_.value < 0)) {
      throw new EnumValidationException("Negative user-provided enum value")
    }

    val seen = new mutable.HashSet[Int]
    var nextValue = 1
    for (v <- vs) {
      if (v.value == 0) {
        v.value = nextValue
      }
      nextValue = v.value + 1
      if (seen contains v.value) {
        throw new EnumValidationException("Repeating enum values")
      }
      seen += v.value
    }
    vs
  }
}

object FieldIdTransformer extends Transformer {
  override def transformFields(fs: List[Field]): List[Field] = {
    if (fs.exists(_.id < 0))
      throw new FieldValidationException("Negative user-provided field id")

    val explicit = fs.filter(_.id != 0)
    if (explicit != explicit.removeDuplicates)
      throw new FieldValidationException("Duplicate user-provided field id")

    var nextId = -1
    for (field <- fs if field.id == 0) {
      field.id = nextId
      nextId -= 1
    }
    fs
  }
}

class Transformer {
  def transform(e: Tree): Tree = e match {
    case Document(hs, ds) =>
      Document(transformHeaders(hs), transformDefinitions(ds))
    case h: Header =>
      h
    case Const(name, tpe, value) =>
      Const(name, transformFieldType(tpe), transformConstValue(value))
    case Typedef(name, tpe) =>
      Typedef(name, transformDefinitionType(tpe))
    case Enum(name, vs) =>
      Enum(name, transformEnumValues(vs))
    case Senum(name, vs) =>
      Senum(name, vs)
    case Struct(name, fs) =>
      Struct(name, transformFields(fs))
    case Exception_(name, fs) =>
      Exception_(name, transformFields(fs))
    case Service(name, parent, fs) =>
      Service(name, parent, transformFunctions(fs))
    case EnumValue(name, value) =>
      EnumValue(name, value)
    case Field(id, name, tpe, default, required, optional) =>
      Field(id, name, transformFieldType(tpe), default.map(transformConstValue), required, optional)
    case Function(name, tpe, args, async, throws) =>
      Function(name, transformFunctionType(tpe), transformFields(args), async, transformFields(throws))
    case MapType(ktpe, vtpe, cpp) =>
      MapType(transformFieldType(ktpe), transformFieldType(vtpe), cpp)
    case SetType(tpe, cpp) =>
      SetType(transformFieldType(tpe), cpp)
    case ListType(tpe, cpp) =>
      ListType(transformFieldType(tpe), cpp)
    case ConstList(vs) =>
      ConstList(transformConstValues(vs))
    case ConstMap(ps) =>
      ConstMap(Map.empty ++ ps.map(scala.Function.tupled((x, y) => (transformConstValue(x), transformConstValue(y)))))
    case catchall => catchall
  }

  def transformHeaders(hs: List[Header]): List[Header] =
    hs.map(h => transform(h).asInstanceOf[Header])
  def transformDefinitions(ds: List[Definition]): List[Definition] =
    ds.map(d => transform(d).asInstanceOf[Definition])
  def transformFieldType(tpe: FieldType): FieldType =
    transform(tpe).asInstanceOf[FieldType]
  def transformConstValue(v: ConstValue): ConstValue =
    transform(v).asInstanceOf[ConstValue]
  def transformDefinitionType(tpe: DefinitionType): DefinitionType =
    transform(tpe).asInstanceOf[DefinitionType]
  def transformEnumValues(vs: List[EnumValue]): List[EnumValue] =
    vs.map(v => transform(v).asInstanceOf[EnumValue])
  def transformFields(fs: List[Field]): List[Field] =
    fs.map(f => transform(f).asInstanceOf[Field])
  def transformFunctions(fs: List[Function]): List[Function] =
    fs.map(f => transform(f).asInstanceOf[Function])
  def transformFunctionType(tpe: FunctionType): FunctionType =
    transform(tpe).asInstanceOf[FunctionType]
  def transformConstValues(vs: List[ConstValue]): List[ConstValue] =
    vs.map(v => transform(v).asInstanceOf[ConstValue])

  def transformDocument(doc: Document) = transform(doc).asInstanceOf[Document]
}
