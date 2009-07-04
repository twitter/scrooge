package com.twitter.scrooge.parser

object ScalaGen {
  def apply(tree: Tree): String = tree match {
    case Document(headers, defs) =>
      filterHeaders(headers).map(apply).mkString("", "\n", "\n") +
      defs.map(apply).mkString("", "\n", "\n")
    case Namespace(_, name) =>
      "package "+name
    case Include(file) =>
      "include "+file+"._"
    case Const(name, tpe, value) =>
      "val "+name+" : "+apply(tpe)+" = "+apply(value)
    case Typedef(name, tpe) =>
      "type "+name+" = "+apply(tpe)
    case Struct(name, fields) =>
      "case class "+name+genFields(fields)
    case Exception(name, fields) =>
      "case class "+name+"Exception"+genFields(fields)+" extends Exception"
    case Service(name, Some(parent), fns) =>
      "trait "+name+" extends "+parent+genFunctions(fns)
    case Service(name, None, fns) =>
      "trait "+name+genFunctions(fns)
    case Function(name, tpe, args, async, throws) =>
      (if (throws.isEmpty) "" else throws.map(apply(_)).mkString("@throws (", ",", ") ")) +
      "def "+name+genFields(args)+" : "+apply(tpe)
    case Field(id, name, tpe, default, required, optional) =>
      name+" : "+apply(tpe)
    case Void => "Unit"
    case TBool => "Boolean"
    case TByte => "Byte"
    case TI16 => "Int"
    case TI32 => "Int"
    case TI64 => "Long"
    case TDouble => "Double"
    case TString => "String"
    case MapType(ktpe, vtpe, _) => "Map["+apply(ktpe)+", "+apply(vtpe)+"]"
    case SetType(tpe, _) => "Set["+apply(tpe)+"]"
    case ListType(tpe, _) => "List["+apply(tpe)+"]"
    case ReferenceType(name) => name
    case IntConstant(i) => i
    case DoubleConstant(d) => d
    case ConstList(elems) => elems.map(apply).mkString("List(", ", ", ")")
    case ConstMap(elems) => elems.map{case (k, v) => apply(k)+" -> "+apply(v)}.mkString("Map(", ", ", ")")
    case StringLiteral(str) => "\""+str+"\""
    case Identifier(name) => name
    case catchall => catchall.toString
  }

  def filterHeaders(headers: List[Header]): List[Header] = {
    val scalaNS = headers.find{case Namespace("scala", _) => true; case _ => false}
    val javaNS = headers.find{case Namespace("java", _) => true; case _ => false}

    (scalaNS orElse javaNS).toList
  }

  def genFunctions(functions: List[Function]): String =
    functions.map(apply).mkString(" {\n  ", "\n  ", "\n}")

  def genFields(fields: List[Field]): String =
    fields.map(apply).mkString("(", ", ", ")")
}
