package com.twitter.scrooge.backend.lua

import com.twitter.scrooge.ast._
import com.twitter.scrooge.backend.{Generator, GeneratorFactory, ServiceOption, TemplateGenerator}
import com.twitter.scrooge.frontend.ResolvedDocument
import com.twitter.scrooge.mustache.Dictionary.{CodeFragment, v}
import com.twitter.scrooge.mustache.HandlebarLoader
import java.io.File

object LuaGeneratorFactory extends GeneratorFactory {

  def luaCommentFunction(commentStyle: HandlebarLoader.CommentStyle): String = {
    import HandlebarLoader._

    commentStyle match {
      case BlockBegin => "--[["
      case BlockContinuation => "  "
      case BlockEnd => "--]]\n"
      case SingleLineComment => "-- "
    }
  }

  val language = "lua"
  val templateLoader = new HandlebarLoader("/luagen/", ".mustache", luaCommentFunction)
  def apply(
    doc: ResolvedDocument,
    defaultNamespace: String,
    experimentFlags: Seq[String]
  ): Generator = new LuaGenerator(
    doc,
    defaultNamespace,
    templateLoader
  )
}

class LuaGenerator(
  val doc: ResolvedDocument,
  val defaultNamespace: String,
  val templateLoader: HandlebarLoader)
    extends TemplateGenerator(doc) {

  import LuaGenerator._

  val namespaceLanguage = "lua"
  val fileExtension = ".lua"
  val experimentFlags = Seq.empty[String]

  def templates: HandlebarLoader = templateLoader

  override def genConstant(constant: RHS, fieldType: Option[FieldType] = None): CodeFragment = {
    constant match {
      case NullLiteral => v("nil")
      case _ => super.genConstant(constant, fieldType)
    }
  }

  def quoteKeyword(str: String): String =
    if (LuaKeywords.contains(str))
      s"_$str"
    else
      str

  override def normalizeCase[N <: Node](node: N): N = {
    (node match {
      case e: EnumField =>
        e.copy(sid = e.sid.toUpperCase)
      case _ => super.normalizeCase(node)
    }).asInstanceOf[N]
  }

  protected override def namespacedFolder(
    destFolder: File,
    namespace: String,
    dryRun: Boolean
  ): File = {
    val file = new File(destFolder, "lua/" + namespace.replace('.', File.separatorChar))
    if (!dryRun) file.mkdirs()
    file
  }

  override def isLazyReadEnabled(t: FunctionType, optional: Boolean): Boolean = false

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

  private[this] def genComponentType(part: String, valueType: FieldType): CodeFragment =
    valueType match {
      case t: ContainerType => v(s"$part = { ${genType(t)} }")
      case t: StructType => v(s"$part = ${genID(t.sid.toTitleCase)}")
      case t: EnumType => v(s"$part = { ttype = 'enum', value = ${genID(t.sid.toTitleCase)} }")
      case _ => v(s"$part = '${genPrimitiveType(valueType)}'")
    }

  def genType(t: FunctionType, immutable: Boolean = false): CodeFragment = t match {
    case bt: BaseType => v(s"ttype = '${genPrimitiveType(bt)}'")
    case StructType(st, _) => v(s"ttype = 'struct', fields = ${genID(st.sid.toTitleCase)}.fields")
    case EnumType(et, _) => v(s"ttype = 'enum', value = ${genID(et.sid.toTitleCase)}")
    case ListType(valueType, _) => v(s"ttype = 'list', ${genComponentType("value", valueType)}")
    case MapType(keyType, valueType, _) =>
      v(
        s"ttype = 'map', ${genComponentType("key", keyType)}, ${genComponentType("value", valueType)}"
      )
    case SetType(valueType, _) => v(s"ttype = 'set', ${genComponentType("value", valueType)}")
    case _ => v("")
  }

  def genPrimitiveType(t: FunctionType): CodeFragment = t match {
    case Void => v("void")
    case TBool => v("bool")
    case TByte => v("byte")
    case TDouble => v("double")
    case TI16 => v("i16")
    case TI32 => v("i32")
    case TI64 => v("i64")
    case TString => v("string")
    case TBinary => v("binary")
    case _ => v("")
  }

  // Not used for Lua
  def genFieldType(f: Field): CodeFragment = v("")

  // For functions (services) -- not supported in Lua
  def genFieldParams(fields: Seq[Field], asVal: Boolean = false): CodeFragment =
    v(
      fields
        .map { f =>
          genID(f.sid).toData
        }
        .mkString(", ")
    )

  // Use "lua" namespace if defined, otherwise default to "java" namespace, but replace "thriftjava"
  // with "thriftlua"
  override def getNamespace(doc: Document): Identifier = {
    def replaceThriftJavaWithThriftLua(s: String) = s.replaceAllLiterally("thriftjava", "thriftlua")

    doc
      .namespace(namespaceLanguage)
      .orElse {
        // If we don't have a lua namespace, fall back to the java one
        doc
          .namespace("java")
          .map {
            case SimpleID(name, origName) =>
              SimpleID(replaceThriftJavaWithThriftLua(name), origName)
            case QualifiedID(names) =>
              QualifiedID(
                names.dropRight(1) ++ names.takeRight(1).map(replaceThriftJavaWithThriftLua)
              )
          }
      }
      .getOrElse(SimpleID(defaultNamespace))
  }

  // Finds all struct types that may be referenced by the given struct or by container types (list,
  // map, set) including nested container types to arbitrary depths.
  // `excludeSelfType` is the SimpleID of the self type such that we avoid adding a require statement
  // for self-type references that were introduced in http://go/rb/873802.
  private[this] def findRequireableStructTypes(
    ft: FieldType,
    excludeSelfType: SimpleID
  ): Seq[NamedType] = {
    ft match {
      case t: NamedType if (excludeSelfType == t.sid) => Nil
      case t: StructType => Seq(t)
      case t: EnumType => Seq(t)
      case ListType(t, _) => findRequireableStructTypes(t, excludeSelfType)
      case MapType(keyType, valueType, _) =>
        findRequireableStructTypes(keyType, excludeSelfType) ++ findRequireableStructTypes(
          valueType,
          excludeSelfType
        )
      case SetType(t, _) => findRequireableStructTypes(t, excludeSelfType)
      case _ => Nil
    }
  }

  private[this] def genRequireStatement(t: NamedType, namespace: Option[Identifier]): String = {
    val typeName = t.sid.toTitleCase.fullName
    val qualifiedName = qualifyNamedType(t, namespace).fullName
    s"local $typeName = require '$qualifiedName'"
  }

  override def structDict(
    struct: StructLike,
    namespace: Option[Identifier],
    includes: Seq[Include],
    serviceOptions: Set[ServiceOption],
    genAdapt: Boolean,
    toplevel: Boolean = false
  ) = {
    val dictionary = super.structDict(struct, namespace, includes, serviceOptions, genAdapt)
    // Struct or Enum types referenced in the struct that need a `require` statement at the top of the lua file
    val requireStatements = struct.fields
      .map(_.fieldType)
      .flatMap(findRequireableStructTypes(_, struct.sid))
      .map(genRequireStatement(_, namespace))
      .distinct
      .sorted
    dictionary.update("requireStatements", requireStatements.mkString("\n"))
    dictionary
  }

  // Finagle support, not implemented
  def genBaseFinagleService = v("")
  def getParentFinagleService(p: ServiceParent): CodeFragment = v("")
  def getParentFinagleClient(p: ServiceParent): CodeFragment = v("")
}

private[this] object LuaGenerator {
  object LuaKeywords {
    private[this] val keywords = Set(
      "and",
      "break",
      "do",
      "else",
      "elseif",
      "end",
      "false",
      "goto",
      "for",
      "function",
      "if",
      "in",
      "local",
      "nil",
      "not",
      "or",
      "repeat",
      "return",
      "then",
      "true",
      "until",
      "while"
    )
    def contains(str: String): Boolean = keywords.contains(str.toLowerCase)
  }
}
