package com.twitter.scrooge.backend.node

import com.twitter.scrooge.ast._
import com.twitter.scrooge.backend.{Generator, GeneratorFactory, ServiceOption, TemplateGenerator}
import com.twitter.scrooge.frontend.ResolvedDocument
import com.twitter.scrooge.mustache.Dictionary.{CodeFragment, v}
import com.twitter.scrooge.mustache.HandlebarLoader

/**
  * Created by nnance on 1/28/17.
  */

object NodeGeneratorFactory extends GeneratorFactory {

  def commentFunction(commentStyle: HandlebarLoader.CommentStyle): String = {
    import HandlebarLoader._

    commentStyle match {
      case BlockBegin => "/**"
      case BlockContinuation => "  "
      case BlockEnd => "*/\n"
      case SingleLineComment => "// "
    }
  }

  val language = "node"
  val templateLoader = new HandlebarLoader("/nodegen/", ".mustache", commentFunction)
  def apply(doc: ResolvedDocument,
            defaultNamespace: String,
            experimentFlags: Seq[String]): Generator =
    new NodeGenerator(doc, defaultNamespace, templateLoader)
}

class NodeGenerator (
  val doc: ResolvedDocument,
  val defaultNamespace: String,
  val templateLoader: HandlebarLoader
) extends TemplateGenerator(doc) {

  import NodeGenerator._

  val namespaceLanguage = "node"
  val fileExtension = ".ts"
  val experimentFlags = Seq.empty[String]
  def templates: HandlebarLoader = templateLoader

  var has_fields = false

  def genType(t: FunctionType): CodeFragment = t match {
    case bt: BaseType => genPrimitiveType(bt)
    case StructType(st, _) => v(s"ttypes.${genID(st.sid.toTitleCase)}")
//    case EnumType(et, _) =>  v(s"ttype = 'enum', value = ${genID(et.sid.toTitleCase)}")
//    case ListType(valueType, _) => v(s"ttype = 'list', ${genComponentType("value", valueType)}")
//    case MapType(keyType, valueType, _) =>
//      v(s"ttype = 'map', ${genComponentType("key", keyType)}, ${genComponentType("value", valueType)}")
//    case SetType(valueType, _) => v(s"ttype = 'set', ${genComponentType("value", valueType)}")
    case _ => v("")
  }


  def genPrimitiveType(t: FunctionType): CodeFragment = t match {
    case Void => v("void")
    case TBool => v("boolean")
    case TByte => v("number")
    case TDouble => v("number")
    case TI16 => v("number")
    case TI32 => v("number")
    case TI64 => v("number")
    case TString => v("string")
    case TBinary => v("string")
    case _ => v("")
  }

  def genFieldType(f: Field): CodeFragment = v("")
  // For functions (services) -- not supported in Lua
  def genFieldParams(fields: Seq[Field], asVal: Boolean = false): CodeFragment = v("")

  def quoteKeyword(str: String): String =
    if (NodeKeywords.contains(str))
      s"_$str"
    else
      str

  override def fieldsToDict(fields: Seq[Field],
                            blacklist: Seq[String],
                            namespace: Option[Identifier] = None) = {

    val dictionaries = super.fieldsToDict(fields, blacklist)

    (dictionaries, fields, 0 until dictionaries.size).zipped.foreach {
      case (dictionary, field, index) =>
        dictionary("fieldNameCamelCase") = genID(field.sid.toCamelCase).toString
        dictionary("fieldNameTitleCase") = genID(field.sid.toTitleCase).toString
        dictionary("fieldTypeTitleCase") = Identifier.toTitleCase(genType(field.fieldType).toString)
        dictionary("isPrimitive") = isPrimitive(field.fieldType) || field.fieldType.isInstanceOf[EnumType]
    }

    dictionaries
  }

  override def structDict(struct: StructLike,
                          namespace: Option[Identifier],
                          includes: Seq[Include], serviceOptions:
                          Set[ServiceOption],
                          toplevel: Boolean = false) = {
    val dictionary = super.structDict(struct, namespace, includes, serviceOptions)
    dictionary("has_fields") = struct.fields.length > 0

    dictionary
  }


  // For constants support, not implemented
  def genList(list: ListRHS, fieldType: Option[FieldType] = None): CodeFragment = v("")
  def genSet(set: SetRHS, fieldType: Option[FieldType]): CodeFragment = v("")
  def genMap(map: MapRHS, fieldType: Option[FieldType] = None): CodeFragment = v("")
  def genEnum(enum: EnumRHS, fieldType: Option[FieldType] = None): CodeFragment = v("")
  def genStruct(struct: StructRHS, fieldType: Option[FieldType] = None): CodeFragment = v("")
  def genUnion(struct: UnionRHS, fieldType: Option[FieldType] = None): CodeFragment = v("")

  // For mutability/immutability support, not implemented
  def genToImmutable(t: FieldType): CodeFragment = v("")
  def genToImmutable(f: Field): CodeFragment = v("")
  def toMutable(t: FieldType): (String, String) = ("", "")
  def toMutable(f: Field): (String, String) = ("", "")

  // finagle support, not implemented
  def genBaseFinagleService = v("")
  def getParentFinagleService(p: ServiceParent): CodeFragment = v("")
  def getParentFinagleClient(p: ServiceParent): CodeFragment = v("")

}

private[this] object NodeGenerator {
  object NodeKeywords {
    private[this] val keywords = Set(
      "break", "case", "catch", "class", "const", "continue", "debugger",
      "default", "delete", "do", "else", "enum", "export", "extends", "false",
      "finally", "for", "function", "if", "import", "in", "instanceof", "new",
      "null", "return", "super", "switch", "this", "throw", "true", "try", "typeof",
      "var", "void", "while", "with")
    def contains(str: String): Boolean = keywords.contains(str.toLowerCase)
  }
}