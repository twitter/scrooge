package com.twitter.scrooge
package javalike

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

import com.twitter.conversions.string._
import com.twitter.handlebar.Dictionary
import AST._
import java.io.File

abstract class JavaLike extends Generator with StructTemplate with ServiceTemplate {
  import Dictionary._

  def outputFile(destFolder: String, doc0: Document, inputFile: String) = {
    val packageDir = new File(destFolder, namespace(doc0).replace('.', File.separatorChar))
    val baseName = AST.stripExtension(new File(inputFile).getName())
    new File(packageDir, baseName + fileExtension)
  }

  def normalizeCase[N <: AST.Node](node: N): N
  def namespace(doc0: Document): String
  val fileExtension: String
  val templateDirName: String

  lazy val templates = new HandlebarLoader(templateDirName, fileExtension)

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

  lazy val header = templates("header").generate {
    doc: Document =>
      val imports = doc.headers.collect {
        case include@AST.Include(_, doc) => (namespace(doc), include.prefix)
      } map {
        case (PackageName(parentPackage, subPackage), prefix) =>
          Dictionary(
            "parentPackage" -> parentPackage,
            "subPackage" -> subPackage,
            "alias" -> prefix)
      }
      Dictionary(
        "namespace" -> v(namespace(doc)),
        "imports" -> v(imports)
      )
  }

  lazy val enumTemplate = templates("enum").generate {
    enum: Enum =>
      val values = enum.values map {
        value =>
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

  lazy val enumsTemplate = templates("enums").generate {
    enums: Seq[Enum] =>
      val enumDictionaries = enums.map(enumTemplate.unpacker)
      Dictionary(
        "hasEnums" -> v(enumDictionaries.nonEmpty),
        "enums" -> v(enumDictionaries),
        "enum" -> v(enumTemplate.handlebar)
      )
  }

  lazy val constsTemplate = templates("consts").generate {
    consts: Seq[Const] =>
      val constants = consts map {
        c =>
          Dictionary(
            "name" -> v(c.name),
            "type" -> v(typeName(c.`type`)),
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
      case c@ListConstant(_) => listValue(c, mutable)
      case c@SetConstant(_) => setValue(c, mutable)
      case c@MapConstant(_) => mapValue(c, mutable)
      case EnumValueConstant(enum, value) => enum.name + "." + value.name
      case Identifier(name) => name
    }
  }

  def listValue(list: ListConstant, mutable: Boolean = false): String
  def setValue(set: SetConstant, mutable: Boolean = false): String
  def mapValue(map: MapConstant, mutable: Boolean = false): String

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
        typeName(`type`, mutable) + "()"
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
  def toImmutable(t: FieldType): String

  /**
   * Generates a suffix to append to a field expression that will
   * convert the value to an immutable equivalent.
   */
  def toImmutable(f: Field): String

  /**
   * Generates a prefix and suffix to wrap around a field expression that will
   * convert the value to a mutable equivalent.
   */
  def toMutable(t: FieldType): (String, String)

  /**
   * Generates a prefix and suffix to wrap around a field expression that will
   * convert the value to a mutable equivalent.
   */
  def toMutable(f: Field): (String, String)

  def typeName(t: FunctionType, mutable: Boolean = false): String

  def fieldTypeName(f: Field, mutable: Boolean = false): String

  def fieldParams(fields: Seq[Field], asVal: Boolean = false): String

  def apply(_doc: Document, serviceOptions: Set[ServiceOption]): String = {
    val doc = normalizeCase(_doc)
    val constSection = constsTemplate(doc.consts)
    val enumSection = enumsTemplate(doc.enums)
    val structSection = doc.structs map {
      x => structTemplate(x)
    } mkString("", "\n\n", "\n\n")
    val serviceSection = doc.services.map {
      x =>
        serviceTemplate(JavaService(x, serviceOptions))
    } mkString("", "\n\n", "\n\n")

    header(doc) + "\n" + constSection + enumSection + structSection + serviceSection
  }
}
