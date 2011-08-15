package com.twitter.scrooge
package scalagen

import java.io.File
import AST._
import org.monkey.mustache.Dictionary

trait ScalaTemplate {
  val paths = List(
    "com.twitter.scrooge",
    "com.twitter.scrooge.scalagen",
    "com.twitter.scrooge.scalagen.StructTemplate",
    "com.twitter.scrooge.scalagen.ServiceTemplate"
  )
  def template[A: Manifest](text: String) = Template[A](text, paths)

  def handlebar[T](name: String)(f: T => Dictionary)(implicit loader: MustacheLoader) =
    new Handlebar(loader(name), f)
}


case class ConstList(constList: Seq[Const])

abstract sealed class ScalaServiceOption
case object WithFinagleClient extends ScalaServiceOption
case object WithFinagleService extends ScalaServiceOption
case object WithOstrichServer extends ScalaServiceOption

case class ScalaService(service: Service, options: Set[ScalaServiceOption])


// maybe should eventually go elsewhere.
class ScalaGenerator extends Generator with ScalaTemplate {
  import StructTemplate._
  import ServiceTemplate._

  implicit val mustaches = new MustacheLoader("/scalagen/")

  val header = handlebar[Document]("header"){ doc =>
    val imports = doc.headers.collect {
      case AST.Include(_, doc) => doc.scalaNamespace
    } filter(_ != doc.scalaNamespace) map { ns =>
      Dictionary().data("namespace", ns)
    }
    Dictionary()
      .data("scalaNamespace", doc.scalaNamespace)
      .dictionaries("imports", imports)
  }

  val enumTemplate = handlebar[Enum]("enum"){ enum =>
    val values = enum.values map { value =>
      Dictionary().data("name", value.name).data("value", value.value.toString)
    }
    Dictionary()
      .data("enum", enum.name)
      .dictionaries("values", values)
  }

  val enumsTemplate = handlebar[Seq[Enum]]("enums"){ enums =>
    val enumDictionaries = enums.map(enumTemplate.f)
    Dictionary()
      .bool("hasEnums", enumDictionaries.nonEmpty)
      .dictionaries("enums", enumDictionaries)
      .partial("enum", enumTemplate.mustache)
  }

  val constsTemplate = handlebar[ConstList]("consts"){ consts =>
    val constants = consts.constList map { c =>
      Dictionary()
        .data("name", c.name)
        .data("type", scalaType(c.`type`))
        .data("value", constantValue(c.`type`, c.value))
    }
    Dictionary()
      .bool("hasConstants", constants.nonEmpty)
      .dictionaries("constants", constants)
  }

  // Constants
  def quote(str: String) = {
    val sb = new StringBuilder(str.length + 20)
    sb.append("\"")
    str foreach {
      case '"' => sb.append("\\\"")
      case '\\' => sb.append("\\")
      case '\n' => sb.append("\\n")
      case '\r' => sb.append("\\r")
      case '\f' => sb.append("\\f")
      case c => sb.append(c)
    }
    sb.append("\"").toString
  }

  def listValue(list: ListConstant): String = {
    "List(" + list.elems.map(constantValue(null, _)).mkString(", ") + ")"
  }

  def mapValue(map: MapConstant): String = {
    "Map(" + (map.elems.map {
      case (k, v) =>
        constantValue(null, k) + " -> " + constantValue(null, v)
    } mkString(", ")) + ")"
  }

  def constantValue(`type`: FieldType, constant: Constant): String = {
    constant match {
      case NullConstant => "null"
      case StringConstant(value) => quote(value)
      case DoubleConstant(value) => value.toString
      case IntConstant(value) => value.toString
      case BoolConstant(value) => value.toString
      case c @ ListConstant(_) => listValue(c)
      case c @ MapConstant(_) => mapValue(c)
      case c @ Identifier(name) =>
        val prefix = `type`.asInstanceOf[NamedType].name + "."
        if (name startsWith prefix) name else prefix + name
    }
  }

  def writeFieldConst(name: String) = name.toUpperCase + "_FIELD_DESC"

  def defaultValueTemplate(field: Field) = {
    field.default.map { d => constantValue(field.`type`, d) }.getOrElse {
      if (field.requiredness.isOptional) {
        "None"
      } else {
        field.`type` match {
          case TBool => "false"
          case TByte | TI16 | TI32 | TI64 => "0"
          case TDouble => "0.0"
          case _ => "null"
        }
      }
    }
  }

  def constType(t: FunctionType): String = {
    t match {
      case Void => "VOID"
      case TBool => "BOOL"
      case TByte => "BYTE"
      case TDouble => "DOUBLE"
      case TI16 => "I16"
      case TI32 => "I32"
      case TI64 => "I64"
      case TString => "STRING"
      case TBinary => "STRING" // thrift's idea of "string" is based on old broken c++ semantics.
      case StructType(_) => "STRUCT"
      case EnumType(_) => "I32" // enums are converted to ints
      case MapType(_, _, _) => "MAP"
      case SetType(_, _) => "SET"
      case ListType(_, _) => "LIST"
      case x => throw new InternalError("constType#" + t)
    }
  }

  def protocolReadMethod(t: FunctionType): String = {
    t match {
      case TBool => "readBool"
      case TByte => "readByte"
      case TI16 => "readI16"
      case TI32 => "readI32"
      case TI64 => "readI64"
      case TDouble => "readDouble"
      case TString => "readString"
      case TBinary => "readBinary"
      case x => throw new InternalError("protocolReadMethod#" + t)
    }
  }

  def protocolWriteMethod(t: FunctionType): String = {
    t match {
      case TBool => "writeBool"
      case TByte => "writeByte"
      case TI16 => "writeI16"
      case TI32 => "writeI32"
      case TI64 => "writeI64"
      case TDouble => "writeDouble"
      case TString => "writeString"
      case TBinary => "writeBinary"
      case x => throw new InternalError("protocolWriteMethod#" + t)
    }
  }

  def scalaType(t: FunctionType): String = {
    t match {
      case Void => "Unit"
      case TBool => "Boolean"
      case TByte => "Byte"
      case TI16 => "Short"
      case TI32 => "Int"
      case TI64 => "Long"
      case TDouble => "Double"
      case TString => "String"
      case TBinary => "ByteBuffer"
      case MapType(k, v, _) => "Map[" + scalaType(k) + ", " + scalaType(v) + "]"
      case SetType(x, _) => "Set[" + scalaType(x) + "]"
      case ListType(x, _) => "Seq[" + scalaType(x) + "]"
      case n: NamedType => n.name
    }
  }

  def scalaFieldType(f: Field): String = {
    if (f.requiredness.isOptional && f.default == None) {
      "Option[" + scalaType(f.`type`) + "]"
    } else {
      scalaType(f.`type`)
    }
  }

  def fieldArgs(args: Seq[Field]): String = {
    args.map { f =>
      val prefix = f.name + ": " + scalaFieldType(f)
      val suffix = f.default.map { d => constantValue(f.`type`, d) } orElse {
        f.requiredness match {
          case Requiredness.Optional => Some("None")
          case _ => None
        }
      } map { " = " + _ }
      prefix + suffix.getOrElse("")
    }.mkString(", ")
  }

  // deprecated (for tests)
  def apply(doc: Document, enum: Enum): String = header(doc) + enumTemplate(enum)
  def apply(doc: Document, consts: ConstList): String = header(doc) + constsTemplate(consts)
  def apply(doc: Document, struct: StructLike): String = header(doc) + structTemplate(struct, this)
  def apply(doc: Document, service: Service): String = header(doc) + serviceTemplate(ScalaService(service, Set()), this)

  def apply(_doc: Document, serviceOptions: Set[ScalaServiceOption]): String = {
    val doc = _doc.camelize

    val constSection = constsTemplate(ConstList(doc.consts))
    val enumSection = enumsTemplate(doc.enums)
    val structSections = doc.structs map { x => structTemplate(x, this) }
    val structSection = structSections.mkString("", "\n\n", "\n\n")
    val serviceSections = doc.services.map { x =>
      serviceTemplate(ScalaService(x, serviceOptions), this)
    }
    val serviceSection = serviceSections.mkString("", "\n\n", "\n\n")

    header(doc) + constSection + enumSection + structSection + serviceSection
  }
}
