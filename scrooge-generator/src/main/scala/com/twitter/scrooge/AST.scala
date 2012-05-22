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

object AST {
  sealed abstract class Node
  sealed abstract class Requiredness extends Node {
    def isOptional = this eq Requiredness.Optional
    def isRequired = this eq Requiredness.Required
    def isDefault = this eq Requiredness.Default
  }
  object Requiredness {
    case object Optional extends Requiredness
    case object Required extends Requiredness
    case object Default extends Requiredness
  }

  sealed abstract class Constant extends Node
  case class BoolConstant(value: Boolean) extends Constant
  case class IntConstant(value: Long) extends Constant
  case class DoubleConstant(value: Double) extends Constant
  case class ListConstant(elems: Seq[Constant]) extends Constant
  case class SetConstant(elems: Set[Constant]) extends Constant
  case class MapConstant(elems: Map[Constant, Constant]) extends Constant
  case class StringConstant(value: String) extends Constant
  case class Identifier(name: String) extends Constant
  case class EnumValueConstant(enum: Enum, value: EnumValue) extends Constant
  case object NullConstant extends Constant

  sealed trait FunctionType extends Node
  case object Void extends FunctionType
  sealed trait FieldType extends FunctionType
  sealed trait BaseType extends FieldType
  case object TBool extends BaseType
  case object TByte extends BaseType
  case object TI16 extends BaseType
  case object TI32 extends BaseType
  case object TI64 extends BaseType
  case object TDouble extends BaseType
  case object TString extends BaseType
  case object TBinary extends BaseType

  trait NamedType extends FieldType {
    def name: String
    def prefix: Option[String]
  }

  case class ReferenceType(name: String) extends FieldType

  case class StructType(struct: StructLike, prefix: Option[String] = None) extends NamedType {
    def name = struct.name
  }

  case class EnumType(enum: Enum, prefix: Option[String] = None) extends NamedType {
    def name = enum.name
  }

  sealed abstract class ContainerType(cppType: Option[String]) extends FieldType
  case class MapType(keyType: FieldType, valueType: FieldType, cppType: Option[String]) extends ContainerType(cppType)
  case class SetType(eltType: FieldType, cppType: Option[String]) extends ContainerType(cppType)
  case class ListType(eltType: FieldType, cppType: Option[String]) extends ContainerType(cppType)

  case class Field(
    id: Int,
    name: String,
    `type`: FieldType,
    default: Option[Constant] = None,
    requiredness: Requiredness = Requiredness.Default)
  extends Node

  case class Function(
    name: String,
    localName: String,
    `type`: FunctionType,
    args: Seq[Field],
    oneway: Boolean,
    throws: Seq[Field])
  extends Node

  object Function {
    def apply(
      name: String,
      `type`: FunctionType,
      args: Seq[Field],
      oneway: Boolean,
      throws: Seq[Field]
    ) = {
      new Function(name, name, `type`, args, oneway, throws)
    }
  }

  sealed abstract class Definition extends Node {
    val name: String
  }

  case class Const(name: String, `type`: FieldType, value: Constant) extends Definition

  case class Typedef(name: String, `type`: FieldType) extends Definition

  case class Enum(name: String, values: Seq[EnumValue]) extends Definition

  case class EnumValue(name: String, value: Int) extends Node

  case class Senum(name: String, values: Seq[String]) extends Definition

  sealed abstract class StructLike extends Definition {
    val fields: Seq[Field]
  }

  case class Struct(name: String, fields: Seq[Field]) extends StructLike

  case class FunctionArgs(name: String, fields: Seq[Field]) extends StructLike

  case class FunctionResult(name: String, fields: Seq[Field]) extends StructLike

  case class Exception_(name: String, fields: Seq[Field]) extends StructLike

  object ServiceParent {
    def apply(service: Service): ServiceParent = ServiceParent(service.name, Some(service))
  }

  case class ServiceParent(name: String, service: Option[Service] = None)

  case class Service(
    name: String,
    parent: Option[ServiceParent],
    functions: Seq[Function]
  ) extends Definition

  sealed abstract class Header extends Node

  case class Include(filename: String, document: Document) extends Header {
    lazy val prefix = stripExtension(filename)
  }

  case class CppInclude(file: String) extends Header

  case class Namespace(scope: String, name: String) extends Header

  case class Document(headers: Seq[Header], defs: Seq[Definition]) extends Node {
    def namespace(language: String) = headers collect {
      case Namespace(l, x) if l == language => x
    } headOption

    def mapNamespaces(namespaceMap: Map[String,String]): Document = {
      copy(
        headers = headers map {
          case header @ Namespace(_, ns) => {
            namespaceMap.get(ns) map {
              newNs => header.copy(name = newNs)
            } getOrElse(header)
          }
          case include @ Include(_, doc) => {
            include.copy(document = doc.mapNamespaces(namespaceMap))
          }
          case header => header
        }
      )
    }

    def consts = defs.collect { case c: Const => c }
    def enums = defs.collect { case e: Enum => e }
    def structs = defs.collect { case s: StructLike => s }
    def services = defs.collect { case s: Service => s }
  }

  def stripExtension(filename: String) = {
    filename.indexOf('.') match {
      case -1 => filename
      case dot => filename.substring(0, dot)
    }
  }
}
