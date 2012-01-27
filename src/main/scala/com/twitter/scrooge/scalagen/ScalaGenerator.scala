/*
 * Copyright 2011 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.scrooge
package scalagen

import java.io.File
import com.twitter.conversions.string._
import com.twitter.handlebar.{Dictionary, Handlebar}
import AST._

case class ConstList(constList: Seq[Const])

abstract sealed class ScalaServiceOption
case object WithFinagleClient extends ScalaServiceOption
case object WithFinagleService extends ScalaServiceOption
case object WithOstrichServer extends ScalaServiceOption

case class ScalaService(service: Service, options: Set[ScalaServiceOption])

class ScalaGenerator extends Generator with StructTemplate with ServiceTemplate {
  import Dictionary._

  implicit val templates = new HandlebarLoader("/scalagen/")

  /**
   * Decomposes a package name into the parent package name and the final sub-package name.
   */
  object PackageName {
    def unapply(str: String): Option[(String, String)] = {
      str.lastIndexOf('.') match {
        case -1 => Some(("_root_", str))
        case lastDot => Some((str.substring(0, lastDot), str.substring(lastDot + 1)))
      }
    }
  }

  val header = templates("header").generate { doc: Document =>
    val imports = doc.headers.collect {
      case include @ AST.Include(_, doc) => (doc.scalaNamespace, include.prefix)
    } map {
      case (PackageName(parentPackage, subPackage), prefix) =>
        Dictionary(
          "parentPackage" -> parentPackage,
          "subPackage" -> subPackage,
          "alias" -> prefix)
    }
    Dictionary(
      "scalaNamespace" -> v(doc.scalaNamespace),
      "imports" -> v(imports)
    )
  }

  val enumTemplate = templates("enum").generate { enum: Enum =>
    val values = enum.values map { value =>
      Dictionary(
        "name" -> v(value.name),
        "nameLowerCase" -> v(value.name.toLowerCase),
        "value" -> v(value.value.toString)
      )
    }
    Dictionary(
      "enum_name" -> v(enum.name),
      "values" -> v(values)
    )
  }

  val enumsTemplate = templates("enums").generate { enums: Seq[Enum] =>
    val enumDictionaries = enums.map(enumTemplate.unpacker)
    Dictionary(
      "hasEnums" -> v(enumDictionaries.nonEmpty),
      "enums" -> v(enumDictionaries),
      "enum" -> v(enumTemplate.handlebar)
    )
  }

  val constsTemplate = templates("consts").generate { consts: ConstList =>
    val constants = consts.constList map { c =>
      Dictionary(
        "name" -> v(c.name),
        "type" -> v(scalaType(c.`type`)),
        "value" -> v(constantValue(c.value))
      )
    }
    Dictionary(
      "hasConstants" -> v(constants.nonEmpty),
      "constants" -> v(constants)
    )
  }

  def quote(str: String) = "\"" + str.quoteC() + "\""

  def constantValue(constant: Constant, mutable: Boolean = false): String = {
    constant match {
      case NullConstant => "null"
      case StringConstant(value) => quote(value)
      case DoubleConstant(value) => value.toString
      case IntConstant(value) => value.toString
      case BoolConstant(value) => value.toString
      case c @ ListConstant(_) => listValue(c, mutable)
      case c @ SetConstant(_) => setValue(c, mutable)
      case c @ MapConstant(_) => mapValue(c, mutable)
      case EnumValueConstant(enum, value) => enum.name + "." + value.name
      case Identifier(name) => name
    }
  }

  def listValue(list: ListConstant, mutable: Boolean = false): String = {
    (if (mutable) "mutable.Buffer(" else "Seq(") +
      list.elems.map(constantValue(_)).mkString(", ") + ")"
  }

  def setValue(set: SetConstant, mutable: Boolean = false): String = {
    (if (mutable) "mutable.Set(" else "Set(") +
      set.elems.map(constantValue(_)).mkString(", ") + ")"
  }

  def mapValue(map: MapConstant, mutable: Boolean = false): String = {
    (if (mutable) "mutable.Map(" else "Map(") + (map.elems.map {
      case (k, v) =>
        constantValue(k) + " -> " + constantValue(v)
    } mkString(", ")) + ")"
  }

  def writeFieldConst(name: String) = TitleCase(name) + "Field"

  /**
   * The default value for the specified type and mutability.
   */
  def defaultValue(`type`: FieldType, mutable: Boolean = false) = {
    `type` match {
      case TBool => "false"
      case TByte | TI16 | TI32 | TI64 => "0"
      case TDouble => "0.0"
      case MapType(_, _, _) | SetType(_, _) | ListType(_, _) =>
        scalaType(`type`, mutable) + "()"
      case _ => "null"
    }
  }

  def defaultFieldValue(f: Field): Option[String] = {
    if (f.requiredness.isOptional) {
      Some("None")
    } else {
      f.default.map(constantValue(_, false)) orElse {
        if (f.`type`.isInstanceOf[ContainerType]) {
          Some(defaultValue(f.`type`))
        } else {
          None
        }
      }
    }
  }

  def defaultReadValue(f: Field): String = {
    defaultFieldValue(f) getOrElse {
      defaultValue(f.`type`, false)
    }
  }

  def defaultMutableFieldValue(f: Field): String = {
    if (f.requiredness.isOptional) {
      "None"
    } else {
      f.default.map(constantValue(_, true)) getOrElse {
        defaultValue(f.`type`, true)
      }
    }
  }

  def isNullableType(t: FieldType, isOptional: Boolean = false) = {
    !isOptional && (
      t match {
        case TBool | TByte | TI16 | TI32 | TI64 | TDouble => false
        case _ => true
      }
    )
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
      case StructType(_, _) => "STRUCT"
      case EnumType(_, _) => "I32" // enums are converted to ints
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

  /**
   * Generates a suffix to append to a field expression that will
   * convert the value to an immutable equivalent.
   */
  def toImmutable(t: FieldType): String = {
    t match {
      case MapType(_, _, _) => ".toMap"
      case SetType(_, _) => ".toSet"
      case ListType(_, _) => ".toList"
      case _ => ""
    }
  }

  /**
   * Generates a suffix to append to a field expression that will
   * convert the value to an immutable equivalent.
   */
  def toImmutable(f: Field): String = {
    if (f.requiredness.isOptional) {
      toImmutable(f.`type`) match {
        case "" => ""
        case underlyingToImmutable => ".map(_" + underlyingToImmutable + ")"
      }
    } else {
      toImmutable(f.`type`)
    }
  }

  /**
   * Generates a prefix and suffix to wrap around a field expression that will
   * convert the value to a mutable equivalent.
   */
  def toMutable(t: FieldType): (String, String)  = {
    t match {
      case MapType(_, _, _) | SetType(_, _) => (scalaType(t, true) + "() ++= ", "")
      case ListType(_, _) => ("", ".toBuffer")
      case _ => ("", "")
    }
  }

  /**
   * Generates a prefix and suffix to wrap around a field expression that will
   * convert the value to a mutable equivalent.
   */
  def toMutable(f: Field): (String, String) = {
    if (f.requiredness.isOptional) {
      toMutable(f.`type`) match {
        case ("", "") => ("", "")
        case (prefix, suffix) => ("", ".map(" + prefix + "_" + suffix + ")")
      }
    } else {
      toMutable(f.`type`)
    }
  }

  def scalaType(t: FunctionType, mutable: Boolean = false): String = {
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
      case MapType(k, v, _) =>
        (if (mutable) "mutable." else "") + "Map[" + scalaType(k) + ", " + scalaType(v) + "]"
      case SetType(x, _) =>
        (if (mutable) "mutable." else "") + "Set[" + scalaType(x) + "]"
      case ListType(x, _) =>
        (if (mutable) "mutable.Buffer" else "Seq") + "[" + scalaType(x) + "]"
      case n: NamedType => n.name
    }
  }

  def scalaFieldType(f: Field, mutable: Boolean = false): String = {
    val baseType = scalaType(f.`type`, mutable)
    if (f.requiredness.isOptional) {
      "Option[" + baseType + "]"
    } else {
      baseType
    }
  }

  def fieldParams(fields: Seq[Field], asVal: Boolean = false): String = {
    fields.map { f =>
      val valPrefix = if (asVal) "val " else ""
      val nameAndType = "`" + f.name + "`: " + scalaFieldType(f)
      val defaultValue = defaultFieldValue(f) map { " = " + _ }
      valPrefix + nameAndType + defaultValue.getOrElse("")
    }.mkString(", ")
  }

  // deprecated (for tests)
  def apply(doc: Document, enum: Enum): String = header(doc) + enumTemplate(enum)
  def apply(doc: Document, consts: ConstList): String = header(doc) + constsTemplate(consts)
  def apply(doc: Document, struct: StructLike): String = header(doc) + structTemplate(struct)
  def apply(doc: Document, service: Service): String = header(doc) + serviceTemplate(ScalaService(service, Set()))

  def apply(_doc: Document, serviceOptions: Set[ScalaServiceOption]): String = {
    val doc = _doc.camelize

    val constSection = constsTemplate(ConstList(doc.consts))
    val enumSection = enumsTemplate(doc.enums)
    val structSection = doc.structs map { x => structTemplate(x) } mkString("", "\n\n", "\n\n")
    val serviceSection = doc.services.map { x =>
      serviceTemplate(ScalaService(x, serviceOptions))
    } mkString("", "\n\n", "\n\n")

    header(doc) + "\n" + constSection + enumSection + structSection + serviceSection
  }
}
