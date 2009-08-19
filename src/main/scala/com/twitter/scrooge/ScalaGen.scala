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
    case s @ Service(name, parent, fns) =>
      genService(s)
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


  def genFields(fields: List[Field]): String =
    fields.map(apply).mkString("(", ", ", ")")

  def genService(service: Service): String = {
    {
      "trait %name%%extends% {\n" +
      service.functions.map(apply).mkString("\n").indent +
      "}\n" +
      "\n" +
      "object %name% {\n" +
      {
        service.functions.map { func => genStruct(Struct(func.name + "_args", func.args)) }.mkString("\n") +
        "\n\n" +
        service.functions.map { func => genStruct(functionToReturnValueStruct(func)) }.mkString("\n") +
        "\n\n"
      }.indent +
      "}\n"
    } % Map("name" -> service.name,
            "extends" -> service.parent.map(" extends " + _).getOrElse(""))
  }

  def functionToReturnValueStruct(func: Function) = {
    val resultFields = (func.tpe match {
      case Void => Nil
      case f: FieldType => List(Field(0, "_rv", f, None, false, true))
    }) ::: func.throws
    Struct(func.name + "_result", resultFields)
  }

  def genStruct(struct: Struct): String = {
    {
      "case class %name%(%vars%) {\n" +
      {
        "def this() = this(%defaults%)\n" +
        "\n" +
        struct.fields.map(fieldId).mkString("\n") + "\n\n" +
        "def decode(f: %name% => Step): Step = Codec.readStruct(this, f) {\n" +
        {
          struct.fields.map(fieldDecoder).mkString("\n") + "\n" +
          "case (_, ftype) => Codec.skip(ftype) { decode(f) }"
        }.indent +
        "}\n" +
        "\n" +
        "def encode(buffer: Buffer) {\n" +
        struct.fields.map(fieldEncoder).mkString("\n").indent +
        "}"
      }.indent +
      "}\n"
    } % Map("name" -> struct.name,
            "vars" -> struct.fields.map(f => "var " + apply(f)).mkString(", "),
            "defaults" -> struct.fields.map(f => defaultForType(f.ftype)).mkString(", "))
  }

  def fieldId(f: Field) = {
    "val F_%uname% = %id%" % Map("uname" -> f.name.toUpperCase, "id" -> f.id.toString)
  }

  def fieldDecoder(f: Field) = {
    "case (F_%uname%, %type%) => %dec% { v => this.%name% = v; decode(f) }" %
      Map("name" -> f.name, "uname" -> f.name.toUpperCase, "type" -> constForType(f.ftype), "dec" -> decoderForType(f.ftype))
  }

  def fieldEncoder(f: Field) = {
    "buffer.writeFieldHeader(FieldHeader(%type%, F_%uname%))\n%enc%" %
      Map("name" -> f.name, "uname" -> f.name.toUpperCase, "type" -> constForType(f.ftype), "enc" -> encoderForType(f.ftype, "this." + f.name))
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
    case MapType(ktpe, vtpe, _) => "mutable.Map.empty[" + apply(ktpe) + ", " + apply(vtpe) + "]"
    case SetType(tpe, _) => "mutable.Set.empty[" + apply(tpe) + "]"
    case ListType(tpe, _) => "List[" + apply(tpe) + "]()"
    case ReferenceType(name) => name + "()"
  }

  def decoderForType(ftype: FieldType): String = ftype match {
    case TBool => "Codec.readBoolean"
    case TByte => "Codec.readByte"
    case TI16 => "Codec.readI16"
    case TI32 => "Codec.readI32"
    case TI64 => "Codec.readI64"
    case TDouble => "Codec.readDouble"
    case TString => "Codec.readString"
    case TBinary => "Codec.readBinary"
    case MapType(ktype, vtype, _) =>
      "Codec.readMap[%k%, %v%](%ktype%, %vtype%) { f => %kdec% { item => f(item) } } { f => %vdec% { item => f(item) } }" %
        Map("k" -> apply(ktype), "v" -> apply(vtype), "ktype" -> constForType(ktype), "vtype" -> constForType(vtype),
            "kdec" -> decoderForType(ktype), "vdec" -> decoderForType(vtype))
    case ListType(itype, _) =>
      "Codec.readList[%i%](%itype%) { f => %dec% { item => f(item) } }" %
        Map("i" -> apply(itype), "itype" -> constForType(itype), "dec" -> decoderForType(itype))
    case SetType(itype, _) =>
      "Codec.readSet[%i%](%itype%) { f => %dec% { item => f(item) } }" %
        Map("i" -> apply(itype), "itype" -> constForType(itype), "dec" -> decoderForType(itype))
    case ReferenceType(name) =>
      "(new %name%).decode" % Map("name" -> name)
  }

  def encoderForType(ftype: FieldType, name: String): String = {
    (ftype match {
      case TBool => "buffer.writeBoolean(%name%)"
      case TByte => "buffer.writeByte(%name%)"
      case TI16 => "buffer.writeI16(%name%)"
      case TI32 => "buffer.writeI32(%name%)"
      case TI64 => "buffer.writeI64(%name%)"
      case TDouble => "buffer.writeDouble(%name%)"
      case TString => "buffer.writeString(%name%)"
      case TBinary => "buffer.writeBinary(%name%)"
      case MapType(ktype, vtype, _) =>
        "buffer.writeMapHeader(%ktype%, %vtype%, %name%.size); for ((k, v) <- %name%) { %kenc%; %venc% }" %
          Map("ktype" -> constForType(ktype), "vtype" -> constForType(vtype),
              "kenc" -> encoderForType(ktype, "k"), "venc" -> encoderForType(vtype, "v"))
      case ListType(itype, _) =>
        "buffer.writeListHeader(%itype%, %name%.size); for (item <- %name%) { %enc% }" %
          Map("itype" -> constForType(itype), "enc" -> encoderForType(itype, "item"))
      case SetType(itype, _) =>
        "buffer.writeSetHeader(%itype, %name%.size); for (item <- %name%) { %enc% }" %
          Map("itype" -> constForType(itype), "enc" -> encoderForType(itype, "item"))
      case ReferenceType(_) => "%name%.encode(buffer)"
    }) % Map("name" -> name)
  }

  def constForType(ftype: FieldType): String = {
    "Type." + (ftype match {
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
    })
  }


  implicit def string2percentString(s: String): PercentString = new PercentString(s)
}

class PercentString(str: String) {
  def %(map: Map[String, String]) = {
    map.foldLeft(str) { (s, item) => item match { case (k, v) => s.replace("%" + k + "%", v) } }
  }

  def indent = {
    str.split("\n").mkString("  ", "\n  ", "\n").replaceAll("(?m)\\s+$", "\n")
  }
}
