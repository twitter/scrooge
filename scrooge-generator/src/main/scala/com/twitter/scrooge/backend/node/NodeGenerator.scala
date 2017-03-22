package com.twitter.scrooge.backend.node

import com.twitter.scrooge.ast._
import com.twitter.scrooge.backend.{Generator, GeneratorFactory, ServiceOption, TemplateGenerator}
import com.twitter.scrooge.frontend.{ScroogeInternalException, ResolvedDocument}
import com.twitter.scrooge.mustache.Dictionary.{CodeFragment, v}
import com.twitter.scrooge.mustache.{Dictionary, HandlebarLoader}
import com.twitter.scrooge.ast._

/**
  * Created by nnance on 1/28/17.
  */

object NodeGeneratorFactory extends GeneratorFactory {
  val language = "node"
  val handlebarLoader = new HandlebarLoader("/nodegen/", ".mustache", commentFunction)
  def apply(
     doc: ResolvedDocument,
     defaultNamespace: String,
     experimentFlags: Seq[String]
   ): Generator = new NodeGenerator(doc, defaultNamespace, handlebarLoader)

  def commentFunction(commentStyle: HandlebarLoader.CommentStyle): String = {
    import HandlebarLoader._

    commentStyle match {
      case BlockBegin => "/**"
      case BlockContinuation => " * "
      case BlockEnd => " */\n"
      case SingleLineComment => "// "
    }
  }
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

  def quoteKeyword(str: String): String =
    if (NodeKeywords.contains(str))
      s"_$str"
    else
      str

  def genType(t: FunctionType): CodeFragment = t match {
    case bt: BaseType => genPrimitiveType(bt)
    case Void => v("void")
    case StructType(st, _) => v(s"${genID(st.sid.toTitleCase)}")
    case MapType(k, vv, _) =>
      v("Map<" + genType(k).toData + ", " + genType(vv).toData + ">")
    case SetType(x, _) =>
      v("Set<" + genType(x).toData + ">")
    case ListType(x, _) =>
      v("Array<" + genType(x).toData + ">")
    case t: NamedType =>
      val id = doc.qualifySimpleID(t.sid, namespaceLanguage, defaultNamespace)
      v(genID(id.toTitleCase).toData)
    case r: ReferenceType =>
      throw new ScroogeInternalException("ReferenceType should not appear in backend")
    case x =>
      if (x.toString().equals("Void")) {
        v("void")
      } else {
        v("any")
      }
  }


  def genPrimitiveType(t: FunctionType): CodeFragment = t match {
    case Void => v("void")
    case TBool => v("boolean")
    case TByte => v("byte")
    case TDouble => v("number")
    case TI16 => v("number")
    case TI32 => v("number")
    case TI64 => v("number")
    case TString => v("string")
    case TBinary => v("Buffer")
    case _ => v("unknownPrimitive")
  }

  def genFieldParams(fields: Seq[Field], asVal: Boolean = false): CodeFragment = v("genFieldParams")

  override def isPrimitive(t: FunctionType): Boolean = {
    def matcher(x: FunctionType) = x match {
      case Void | TString | TBool | TByte | TI16 | TI32 | TI64 | TDouble => true
      case _ => false
    }
    matcher(t) || t.isInstanceOf[EnumType]
  }

  override def fieldsToDict(fields: Seq[Field],
                            blacklist: Seq[String],
                            namespace: Option[Identifier] = None) = {

    val dictionaries = super.fieldsToDict(fields, blacklist)

    (dictionaries, fields, 0 until dictionaries.size).zipped.foreach {
      case (dictionary, field, index) =>
        dictionary("typeTitleCase") = Identifier.toTitleCase(genType(field.fieldType).toString)
        dictionary("noNamespaceFieldType") = genType(field.fieldType).toString.replaceAll(""".*\.""", "")

        dictionary("fieldNameCamelCase") = genID(field.sid.toCamelCase).toString
        dictionary("fieldNameTitleCase") = genID(field.sid.toTitleCase).toString
        dictionary("fieldTypeTitleCase") = Identifier.toTitleCase(genType(field.fieldType).toString)
        dictionary("isPrimitive") = isPrimitive(field.fieldType)
    }

    dictionaries
  }

  override def getNamespace(doc: Document): Identifier = {
    def replaceThriftJavaWithThriftLua(s: String) = s.replaceAllLiterally("thriftjava", "thriftnode")

    doc.namespace(namespaceLanguage)
      .orElse {
        // If we don't have a lua namespace, fall back to the java one
        doc
          .namespace("java")
          .map {
            case SimpleID(name, origName) => SimpleID(replaceThriftJavaWithThriftLua(name), origName)
            case QualifiedID(names) => QualifiedID(names.dropRight(1) ++ names.takeRight(1).map(replaceThriftJavaWithThriftLua))
          }
      }
      .getOrElse(SimpleID(defaultNamespace))
  }

  private[this] def findRequireableStructTypes(ft: FieldType, excludeSelfType: SimpleID): Seq[NamedType] = {
    ft match {
      case t: NamedType if (excludeSelfType == t.sid) => Nil
      case t: StructType => Seq(t)
      case t: EnumType => Seq(t)
      case ListType(t, _) => findRequireableStructTypes(t, excludeSelfType)
      case MapType(keyType, valueType, _) => findRequireableStructTypes(keyType, excludeSelfType) ++ findRequireableStructTypes(valueType, excludeSelfType)
      case SetType(t, _) => findRequireableStructTypes(t, excludeSelfType)
      case _ => Nil
    }
  }

  private[this] def genRequireStatement(t: NamedType, namespace: Option[Identifier]): String = {
    val typeName = t.sid.toTitleCase.fullName
//    val qualifiedName = qualifyNamedType(t, namespace).fullName
    s"import {$typeName} from './$typeName'"
  }

  override def structDict(struct: StructLike,
                          namespace: Option[Identifier],
                          includes: Seq[Include], serviceOptions:
                          Set[ServiceOption],
                          toplevel: Boolean = false) = {
    val dictionary = super.structDict(struct, namespace, includes, serviceOptions)

    val requireStatements = struct
      .fields
      .map(_.fieldType)
      .flatMap(findRequireableStructTypes(_, struct.sid))
      .map(genRequireStatement(_, namespace))
      .distinct
      .sorted

    dictionary.update("requireStatements", requireStatements.mkString("\n"))
    dictionary("importDoc") = true
    dictionary
  }

  override def functionDictionary(function: Function, generic: Option[String]) = {
    val dict = super.functionDictionary(function, generic)
    dict("name") = genID(function.funcName).toString
    dict("funcNameTitleCase") = genID(function.funcName.toTitleCase).toString
    dict("nameTitleCase") = genID(function.funcName.toTitleCase).toString
    dict("args") = fieldsToDict(function.args, List.empty)
    dict("argsLength") = function.args.length.toString
    dict("resultType") = {
      val funcType = genType(function.funcType)
      if (isPrimitive(function.funcType)) {
        "args.success"
      } else {
        s"new $funcType(args.success)"
      }
    }

    dict
  }

  override def serviceDict(service: Service,
                           namespace: Identifier,
                           includes: Seq[Include],
                           options: Set[ServiceOption]): Dictionary = {
    val dict = super.serviceDict(service, namespace, includes, options)
    dict("syncFunctionStructs") = genSyncFunctionStructs(service, dict)
    val doc = normalizeCase(resolvedDoc.document)
    dict("structs") = doc.structs.map { struct =>
      structDict(struct, Some(namespace), includes, options, true)
    }

    dict
  }

  def genSyncFunctionStructs(service: Service, dict: Dictionary): Seq[Dictionary] = {
    service.functions.map(f => {
      val baseClassName = service.sid.fullName + f.funcName.toTitleCase.fullName
      val argsStruct = Dictionary(
        "StructName" -> v(baseClassName + "Args"),
        "fields" -> v(fieldsToDict(f.args, Nil)),
        "has_fields" -> v(f.args.nonEmpty)
      )


      val successFieldType = functionTypeToFieldType(f.funcType)
      val successField: Seq[Field] = successFieldType.map(Field(0, SimpleID("success", Some("success")), "success", _)).toSeq
      val resultFields = f.throws ++ successField
      val resultFieldsDict = fieldsToDict(resultFields, Nil)
      val resultStruct = Dictionary(
        "StructName" -> v(baseClassName + "Result"),
        "has_fields" -> v(!f.funcType.isInstanceOf[Void.type]),
        "fields" -> v(resultFieldsDict)
      )

      Dictionary(
        "argsStruct" -> v(argsStruct),
        "resultStruct" -> v(resultStruct)
      )
    })
  }

  def genList(list: ListRHS, fieldType: Option[FieldType] = None): CodeFragment = {
    val listElemType = fieldType.map(_.asInstanceOf[ListType].eltType)
    val code = list.elems.map { e =>
      genConstant(e, listElemType).toData
    }.mkString(", ")
    v(s"[$code]")
  }

  def genSet(set: SetRHS, fieldType: Option[FieldType] = None): CodeFragment = {
    val setElemType = fieldType.map(_.asInstanceOf[SetType].eltType)
    val code = set.elems.map { e =>
      genConstant(e, setElemType).toData
    }.mkString(", ")
    v(s"Set([$code])")
  }

  def genMap(map: MapRHS, fieldType: Option[FieldType] = None): CodeFragment = {
    val mapType = fieldType.map(_.asInstanceOf[MapType])
    val code = map.elems.map { case (k, v) =>
      val key = genConstant(k, mapType.map(_.keyType)).toData
      val value = genConstant(v, mapType.map(_.valueType)).toData
      s"[$key, $value]"
    }.mkString(", ")

    v(s"Map([$code])")
  }

  def genEnum(enum: EnumRHS, fieldType: Option[FieldType] = None): CodeFragment = {
    def getTypeId: Identifier = fieldType.getOrElse(Void) match {
      case n: NamedType => qualifyNamedType(n)
      case _ =>  enum.enum.sid
    }
    genID(enum.value.sid.toTitleCase.addScope(getTypeId.toTitleCase))
  }

    // For constants support, not implemented
//  def genEnum(enum: EnumRHS, fieldType: Option[FieldType] = None): CodeFragment = v("")
  def genStruct(struct: StructRHS, fieldType: Option[FieldType] = None): CodeFragment = v("struct")
  def genUnion(struct: UnionRHS, fieldType: Option[FieldType] = None): CodeFragment = v("union")

  // For mutability/immutability support, not implemented
  def genToImmutable(t: FieldType): CodeFragment = v("")
  def genToImmutable(f: Field): CodeFragment = v("")
  def toMutable(t: FieldType): (String, String) = ("", "")
  def toMutable(f: Field): (String, String) = ("", "")

  // finagle support, not implemented
  def genBaseFinagleService = v("")
  def getParentFinagleService(p: ServiceParent): CodeFragment = v("")
  def getParentFinagleClient(p: ServiceParent): CodeFragment = v("")

  def genFieldType(f: Field): CodeFragment = {
    val baseType = genType(f.fieldType).toData
    val code =
      if (f.requiredness.isOptional) {
        baseType + "| null"
      } else {
        baseType
      }
    v(code)
  }

  def functionTypeToFieldType(t: FunctionType): Option[FieldType] = t match {
    case x: FieldType => Some(x)
    case Void => None
    case OnewayVoid => None
  }
}