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

import AST._
import com.twitter.handlebar.{Dictionary, Handlebar}

trait StructTemplate extends Generator { self: ScalaGenerator =>
  import Dictionary._

  // ----- readers

  lazy val readBasicTemplate = templates("readBasic").generate { t: FieldType =>
    Dictionary(
      "protocolReadMethod" -> protocolReadMethod(t)
    )
  }

  lazy val readListTemplate = templates("readList").generate { t: ListType =>
    Dictionary(
      // FIXME remove corba "elt" jargon
      "eltType" -> v(scalaType(t.eltType)),
      "eltReader" -> v(readTemplate(t.eltType).indent(2))
    )
  }

  lazy val readSetTemplate = templates("readSet").generate { t: SetType =>
    Dictionary(
      "eltType" -> v(scalaType(t.eltType)),
      "eltReader" -> v(readTemplate(t.eltType).indent(2))
    )
  }

  lazy val readMapTemplate = templates("readMap").generate { t: MapType =>
    Dictionary(
      "keyType" -> v(scalaType(t.keyType)),
      "valueType" -> v(scalaType(t.valueType)),
      "keyReader" -> v(readTemplate(t.keyType).indent(2)),
      "valueReader" -> v(readTemplate(t.valueType).indent(2))
    )
  }

  lazy val readStructTemplate = templates("readStruct").generate { t: StructType =>
    Dictionary(
      "name" -> v(t.name)
    )
  }

  lazy val readEnumTemplate = templates("readEnum").generate { t: EnumType =>
    Dictionary(
      "name" -> v(t.name)
    )
  }

  def readTemplate(t: FieldType): String = {
    t match {
      case x: ListType => readListTemplate(x)
      case x: SetType => readSetTemplate(x)
      case x: MapType => readMapTemplate(x)
      case x: StructType => readStructTemplate(x)
      case x: EnumType => readEnumTemplate(x)
      case x: BaseType => readBasicTemplate(x)
    }
  }

  lazy val readFieldTemplate = templates("readField").generate { f: Field =>
    Dictionary(
      "id" -> v(f.id.toString),
      "name" -> v(f.name),
      "constType" -> v(constType(f.`type`)),
      "optionality" -> v(if (f.requiredness.isOptional) "Some" else ""),
      "valueReader" -> v(readTemplate(f.`type`).indent(4)),
      "required" -> v(f.requiredness.isRequired)
    )
  }

  // ----- writers

  lazy val writeBasicTemplate = templates("writeBasic").generate { t: FieldType =>
    Dictionary(
      "protocolWriteMethod" -> v(protocolWriteMethod(t))
    )
  }

  lazy val writeListTemplate = templates("writeList").generate { t: ListType =>
    Dictionary(
      "eltType" -> v(constType(t.eltType)),
      "eltWriter" -> v(writeTemplate(t.eltType).indent(1))
    )
  }

  lazy val writeSetTemplate = templates("writeSet").generate { t: SetType =>
    Dictionary(
      "eltType" -> v(constType(t.eltType)),
      "eltWriter" -> v(writeTemplate(t.eltType).indent(1))
    )
  }

  lazy val writeMapTemplate = templates("writeMap").generate { t: MapType =>
    Dictionary(
      "keyType" -> v(constType(t.keyType)),
      "valueType" -> v(constType(t.valueType)),
      "keyWriter" -> v(writeTemplate(t.keyType).indent(2)),
      "valueWriter" -> v(writeTemplate(t.valueType).indent(2))
    )
  }

  lazy val writeStructTemplate = templates("writeStruct").generate { t: StructType =>
    Dictionary()
  }

  lazy val writeEnumTemplate = templates("writeEnum").generate { t: EnumType =>
    Dictionary()
  }

  def writeTemplate(t: FieldType): String = {
    t match {
      case x: ListType => writeListTemplate(x)
      case x: SetType => writeSetTemplate(x)
      case x: MapType => writeMapTemplate(x)
      case x: StructType => writeStructTemplate(x)
      case x: EnumType => writeEnumTemplate(x)
      case x: BaseType => writeBasicTemplate(x)
    }
  }

  lazy val writeFieldTemplate = templates("writeField").generate { f: Field =>
    val conditional = if (f.requiredness.isOptional) {
      "`" + f.name + "`.isDefined"
    } else {
      f.`type` match {
        case AST.TBool | AST.TByte | AST.TI16 | AST.TI32 | AST.TI64 | AST.TDouble =>
          "true"
        case _ =>
          "`" + f.name + "` ne null"
      }
    }
    Dictionary(
      "name" -> v(f.name),
      "conditional" -> v(conditional),
      "fieldConst" -> v(writeFieldConst(f.name)),
      "getter" -> v(if (f.requiredness.isOptional) ".get" else ""),
      "valueWriter" -> v(writeTemplate(f.`type`).indent(1))
    )
  }

  // ----- struct

  lazy val structTemplate = templates("struct").generate { struct: StructLike =>
    val isBig = struct.fields.size > 22
    val fieldNames = struct.fields.map { f => "`" + f.name + "`" }
    val fieldDictionaries = struct.fields.zipWithIndex map { case (field, index) =>
      Dictionary(
        "index" -> v(index.toString),
        "name" -> v(field.name),
        "id" -> v(field.id.toString),
        "fieldConst" -> v(writeFieldConst(field.name)),
        "constType" -> v(constType(field.`type`)),
        "scalaType" -> v(scalaFieldType(field)),
        "defaultReadValue" -> v(defaultReadValue(field)),
        "required" -> v(field.requiredness.isRequired),
        "reader" -> v(readFieldTemplate(field).indent(5)),
        "writer" -> v(writeFieldTemplate(field).indent(2)),
        "struct" -> v(struct.name)
      )
    }
    val optionalDefaultDictionaries = struct.fields.filter { f =>
      f.requiredness.isOptional && f.default.isDefined
    } map { f =>
      Dictionary(
        "name" -> v(f.name),
        "value" -> v(constantValue(f.default.get))
      )
    }
    val parentType = struct match {
      case AST.Struct(_, _) => "ThriftStruct"
      case AST.Exception_(_, _) => "Exception with ThriftStruct with SourcedException"
    }
    val baseDict = Dictionary(
      "name" -> v(struct.name),
      "fieldNames" -> v(fieldNames.mkString(", ")),
      "fieldParams" -> v(fieldParams(struct.fields, isBig)),
      "parentType" -> v(parentType),
      "fields" -> v(fieldDictionaries),
      "optionalDefaults" -> v(optionalDefaultDictionaries),
      "big" -> v(isBig)
    )

    if (isBig) {
      baseDict("applyParams") = fieldParams(struct.fields)
      baseDict("ctorArgs") = fieldNames.map { name => name + " = " + name }.mkString(", ")
      baseDict("copyParams") = copyParams(struct.fields)
      baseDict("fieldsEqual") = fieldNames.map { name => "this." + name + " == _that." + name}.mkString(" && ")
      baseDict("arity") =  struct.fields.size.toString
    }
    baseDict
  }
}
