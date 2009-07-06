package com.twitter.scrooge

object ScalaGen {
  def apply(tree: Tree): String = tree match {
    case Document(headers, defs) =>
      filterHeaders(headers).map(apply).mkString("", "\n", "\n") +
        defs.map(apply).mkString("", "\n", "\n")
    case Namespace(namespace, name) =>
      "package " + name
    case Include(filename, document) =>
      "\n" + apply(document) + "\n"
    case Const(name, tpe, value) =>
      "val " + name + ": " + apply(tpe) + " = " + apply(value)
    case Typedef(name, tpe) =>
      "type " + name + " = " + apply(tpe)
    case s @ Struct(name, fields) =>
      genStruct(s)
    case Exception(name, fields) =>
      "case class " + name + "Exception" + genFields(fields) + " extends Exception"
    case Service(name, Some(parent), fns) =>
      "trait " + name + " extends " + parent + genFunctions(fns)
    case Service(name, None, fns) =>
      "trait " + name + genFunctions(fns)
    case Function(name, tpe, args, async, throws) =>
      (if (throws.isEmpty) "" else throws.map { field => apply(field.ftype) }.mkString("@throws(", ",", ") ")) +
        "def " + name + genFields(args) + ": " + apply(tpe)
    case Field(id, name, tpe, default, required, optional) =>
      name + ": " + apply(tpe)
    case Void => "Unit"
    case TBool => "Boolean"
    case TByte => "Byte"
    case TI16 => "Int"
    case TI32 => "Int"
    case TI64 => "Long"
    case TDouble => "Double"
    case TString => "String"
    case TBinary => "Array[Byte]"
    case MapType(ktpe, vtpe, _) => "Map[" + apply(ktpe) + ", " + apply(vtpe) + "]"
    case SetType(tpe, _) => "Set[" + apply(tpe) + "]"
    case ListType(tpe, _) => "Seq[" + apply(tpe) + "]"
    case ReferenceType(name) => name
    case IntConstant(i) => i
    case DoubleConstant(d) => d
    case ConstList(elems) => elems.map(apply).mkString("List(", ", ", ")")
    case ConstMap(elems) => elems.map { case (k, v) => apply(k) + " -> " + apply(v) }.mkString("Map(", ", ", ")")
    case StringLiteral(str) => "\"" + str + "\""
    case Identifier(name) => name
    case Enum(name, values) =>
      "object " + name + " {\n" + values.toString + "\n}\n"
    case catchall => catchall.toString
  }

  def filterHeaders(headers: List[Header]): List[Header] = {
    headers filter {
      case Namespace("scala", _) => true
      case Namespace("java", _) => true
      case Namespace(_, _) => false
      case _ => true
    }
  }

  def genFunctions(functions: List[Function]): String =
    functions.map(apply).mkString(" {\n  ", "\n  ", "\n}")

  def genFields(fields: List[Field]): String =
    fields.map(apply).mkString("(", ", ", ")")

  def genStruct(struct: Struct): String = {
    "case class " + struct.name + struct.fields.map(f => "var " + apply(f)).mkString("(", ", ", ")") + " {\n" +
    "  // empty constructor for decoding\n" +
    "  def this() = this" + struct.fields.map(f => defaultForType(f.ftype)).mkString("(", ", ", ")") + "\n" +
    "\n" +
    struct.fields.map { f =>
      "  val F_" + f.name.toUpperCase + " = " + f.id
    }.mkString("\n") + "\n\n" +
    "  def decode(f: " + struct.name + " => Step): Step = Codec.readStruct(this, f) {\n" +
    struct.fields.map { f =>
      "    case (F_" + f.name.toUpperCase + ", Type." + constForType(f.ftype) + ") => " +
      "Codec." + decoderForType(f.ftype) + " { v => this." + f.name + " = v; decode(f) }"
    }.mkString("\n") + "\n" +
    "    case (_, ftype) => Codec.skip(ftype) { decode(f) }\n" +
    "  }\n" +
    "}\n"
  }

  def defaultForType(ftype: FieldType): String = ftype match {
    case TBool => "false"
    case TByte => "0.toByte"
    case TI16 => "0"
    case TI32 => "0"
    case TI64 => "0L"
    case TDouble => "0.0"
    case TString => "null"
    case TBinary => "null"
    case MapType(ktpe, vtpe, _) => "Map.empty[" + apply(ktpe) + ", " + apply(vtpe) + "]"
    case SetType(tpe, _) => "Set.empty[" + apply(tpe) + "]"
    case ListType(tpe, _) => "List[" + apply(tpe) + "]()"
    case ReferenceType(name) => name + "()"
  }

  def decoderForType(ftype: FieldType): String = ftype match {
    case TBool => "readBoolean"
    case TByte => "readByte"
    case TI16 => "readI16"
    case TI32 => "readI32"
    case TI64 => "readI64"
    case TDouble => "readDouble"
    case TString => "readString"
    case TBinary => "readBinary"
    // FIXME:
    case MapType(ktpe, vtpe, _) => "Map.empty[" + apply(ktpe) + ", " + apply(vtpe) + "]"
    case SetType(tpe, _) => "Set.empty[" + apply(tpe) + "]"
    case ListType(tpe, _) => "List[" + apply(tpe) + "]()"
    case ReferenceType(name) => name + "()"
  }

  def constForType(ftype: FieldType): String = ftype match {
    case TBool => "BOOL"
    case TByte => "BYTE"
    case TI16 => "I16"
    case TI32 => "I32"
    case TI64 => "I64"
    case TDouble => "DOUBLE"
    case TString => "STRING"
    case TBinary => "STRING"
    case MapType(_, _, _) => "MAP"
    case SetType(_, _) => "SET"
    case ListType(_, _) => "LIST"
    case ReferenceType(_) => "STRUCT"
  }
}
