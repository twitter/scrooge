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

import com.twitter.scrooge.ast._
import scala.collection.mutable.ArrayBuffer

class TypeNotFoundException(name: String) extends Exception(name)
class UndefinedSymbolException(name: String) extends Exception(name)
class TypeMismatchException(name: String) extends Exception(name)

case class ResolvedDocument(document: Document, resolver: TypeResolver)
case class ResolvedDefinition(definition: Definition, resolver: TypeResolver)

object TypeResolver {
  class EntityResolver[T](
    typeMap: Map[String,T],
    includeMap: Map[String, ResolvedDocument],
    next: TypeResolver => EntityResolver[T])
  {
    def apply(name: String): T = {
      name match {
        case QualifiedName(prefix, suffix) =>
          apply(prefix, suffix)
        case _ =>
          typeMap.get(name).getOrElse {
            throw new TypeNotFoundException(name)
          }
      }
    }

    def apply(scope: String, name: String): T = {
      val include = includeMap.get(scope).getOrElse(throw new UndefinedSymbolException(name))
      try {
        next(include.resolver)(name) match {
          case st: StructType => st.copy(prefix = Some(scope)).asInstanceOf[T]
          case et: EnumType => et.copy(prefix = Some(scope)).asInstanceOf[T]
          case t => t
        }
      } catch {
        case ex: TypeNotFoundException =>
          // don't lose context
          throw new TypeNotFoundException(scope + "." + name)
      }
    }
  }

  object QualifiedName {
    def unapply(str: String): Option[(String, String)] = {
      str.indexOf('.') match {
        case -1 => None
        case dot => Some((str.substring(0, dot), str.substring(dot + 1)))
      }
    }
  }
}

case class TypeResolver(
  typeMap: Map[String, FieldType] = Map(),
  constMap: Map[String, Const] = Map(),
  serviceMap: Map[String, Service] = Map(),
  includeMap: Map[String, ResolvedDocument] = Map())
{
  import TypeResolver._

  lazy val fieldTypeResolver: EntityResolver[FieldType] =
    new EntityResolver(typeMap, includeMap, _.fieldTypeResolver)
  lazy val serviceResolver: EntityResolver[Service] =
    new EntityResolver(serviceMap, includeMap, _.serviceResolver)
  lazy val constResolver: EntityResolver[Const] =
    new EntityResolver(constMap, includeMap, _.constResolver)

  /**
   * Resolves all types in the given document.
   */
  def resolve(doc: Document, forcePrefix: Option[String] = None): ResolvedDocument = {
    var resolver = this
    val includes = doc.headers.collect { case i: Include => i }
    val defBuf = new ArrayBuffer[Definition](doc.defs.size)

    for (i <- includes) {
      resolver = resolver.include(i)
    }

    for (d <- doc.defs) {
      val ResolvedDefinition(d2, r2) = resolver.resolve(d, forcePrefix)
      resolver = r2
      defBuf += d2
    }

    ResolvedDocument(doc.copy(defs = defBuf.toSeq), resolver)
  }

  /**
   * Returns a new TypeResolver with the given include mapping added.
   */
  def include(inc: Include): TypeResolver = {
    val resolvedDocument = TypeResolver().resolve(inc.document, Some(inc.prefix))
    copy(includeMap = includeMap + (inc.prefix -> resolvedDocument))
  }

  /**
   * Resolves types in the given definition according to the current
   * typeMap, and then returns an updated TypeResolver with the new
   * definition bound, plus the resolved definition.
   */
  def resolve(definition: Definition, forcePrefix: Option[String]): ResolvedDefinition = {
    apply(definition) match {
      case d @ Typedef(name, t) => ResolvedDefinition(d, define(name, t))
      case e @ Enum(name, _, _) => ResolvedDefinition(e, define(name, EnumType(e)))
      case s @ Senum(name, _) => ResolvedDefinition(s, define(name, TString))
      case s @ Struct(name, _, _) => ResolvedDefinition(s, define(name, StructType(s, prefix = forcePrefix)))
      case e @ Exception_(name, _, _) => ResolvedDefinition(e, define(e.name, StructType(e)))
      case c @ Const(_, _, v, _) => ResolvedDefinition(c, define(c))
      case s @ Service(name, _, _, _) => ResolvedDefinition(s, define(s))
      case d => ResolvedDefinition(d, this)
    }
  }

  /**
   * Returns a new TypeResolver with the given type mapping added.
   */
  def define(name: String, `type`: FieldType): TypeResolver = {
    copy(typeMap = typeMap + (name -> `type`))
  }

  /**
   * Returns a new TypeResolver with the given constant added.
   */
  def define(const: Const): TypeResolver = {
    copy(constMap = constMap + (const.name -> const))
  }

  /**
   * Returns a new TypeResolver with the given service added.
   */
  def define(service: Service): TypeResolver = {
    copy(serviceMap = serviceMap + (service.name -> service))
  }

  def apply(definition: Definition): Definition = {
    definition match {
      case d @ Typedef(name, t) => d.copy(`type` = apply(t))
      case s @ Struct(_, fs, _) => s.copy(fields = fs.map(apply))
      case e @ Exception_(_, fs, _) => e.copy(fields = fs.map(apply))
      case c @ Const(_, t, _, _) =>
        val `type` = apply(t)
        c.copy(`type` = `type`, value = apply(c.value, `type`))
      case s @ Service(_, p, fs, _) => s.copy(parent = p.map(apply), functions = fs.map(apply))
      case d => d
    }
  }

  def apply(f: Function): Function = f match {
    case Function(_, _, t, as, ts, _) =>
      f.copy(`type` = apply(t), args = as.map(apply), throws = ts.map(apply))
  }

  def apply(f: Field): Field = {
    val fieldType = apply(f.`type`)
    f.copy(
      `type` = fieldType,
      default = f.default.map { const => apply(const, fieldType) })
  }

  def apply(t: FunctionType): FunctionType = t match {
    case Void => Void
    case t: FieldType => apply(t)
  }

  def apply(t: FieldType): FieldType = t match {
    case ReferenceType(name) => apply(name)
    case m @ MapType(k, v, _) => m.copy(keyType = apply(k), valueType = apply(v))
    case s @ SetType(e, _) => s.copy(eltType = apply(e))
    case l @ ListType(e, _) => l.copy(eltType = apply(e))
    case _ => t
  }

  def apply(c: Constant, fieldType: FieldType): Constant = c match {
    case l @ ListConstant(elems) =>
      fieldType match {
        case ListType(eltType, _) => l.copy(elems = elems map { e => apply(e, eltType) } )
        case SetType(eltType, _) => SetConstant(elems map { e => apply(e, eltType) } toSet)
        case _ => throw new TypeMismatchException("Expecting " + fieldType + ", found " + l)
      }
    case m @ MapConstant(elems) =>
      fieldType match {
        case MapType(keyType, valType, _) =>
          m.copy(elems = elems.map { case (k, v) => (apply(k, keyType), apply(v, valType)) })
        case _ => throw new TypeMismatchException("Expecting " + fieldType + ", found " + m)
      }
    case i @ Identifier(name) =>
      fieldType match {
        case EnumType(enum, _) =>
          val valueName = name match {
            case QualifiedName(scope, QualifiedName(enumName, valueName)) =>
              if (fieldTypeResolver(scope, enumName) != fieldType) {
                throw new UndefinedSymbolException(scope + "." + enumName)
              } else {
                valueName
              }
            case QualifiedName(enumName, valueName) =>
              if (enumName != enum.name) {
                throw new UndefinedSymbolException(enumName)
              } else {
                valueName
              }
            case _ => name
          }
          enum.values.find(_.name == valueName) match {
            case None => throw new UndefinedSymbolException(name)
            case Some(value) => EnumValueConstant(enum, value)
          }
        case t: BaseType =>
          name match {
            case QualifiedName(scope, valueName) =>
              val const = constResolver(scope, valueName)
              if (const.`type` != fieldType) {
                throw new UndefinedSymbolException(scope + "." + valueName)
              } else {
                const.value
              }
            case _ => throw new UndefinedSymbolException(name)
          }
        case _ => throw new UndefinedSymbolException(name)
      }
    case _ => c
  }

  def apply(parent: ServiceParent): ServiceParent = {
    parent.copy(service = Some(serviceResolver(parent.name)))
  }

  def apply(name: String): FieldType = fieldTypeResolver(name)
}
