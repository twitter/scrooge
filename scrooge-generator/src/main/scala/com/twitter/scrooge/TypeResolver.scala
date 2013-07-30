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
class UndefinedConstantException(name: String) extends Exception(name)
class UndefinedSymbolException(name: String) extends Exception(name)
class TypeMismatchException(name: String) extends Exception(name)

case class ResolvedDocument(document: Document, resolver: TypeResolver)
case class ResolvedDefinition(definition: Definition, resolver: TypeResolver)

abstract class EntityResolver[T](
  entityMap: Map[String,T],
  includeMap: Map[String, ResolvedDocument],
  next: TypeResolver => EntityResolver[T])
{
  def createMissingEntityException(name: String): Exception

  def apply(id: Identifier): T = id match {
    case SimpleID(name) =>
      entityMap.get(name).getOrElse {
        throw createMissingEntityException(name)
      }
    case qid: QualifiedID =>
      // todo CSL-272:
      // qid can be:
      //   - includeName.constName
      //   - enumName.fieldName
      //   - includeName.enumName.fieldName?
      // I don't think the latter two cases will work.
      val (root, rest) = (qid.names.head, qid.names.tail.mkString("."))
      val include = includeMap.get(root).getOrElse(throw new UndefinedSymbolException(rest))
      try {
        next(include.resolver)(Identifier(rest)) match {
          case st: StructType => st.copy(scopePrefix = Some(SimpleID(root))).asInstanceOf[T]
          case et: EnumType => et.copy(scopePrefix = Some(SimpleID(root))).asInstanceOf[T]
          case t => t
        }
      } catch {
        case ex: UndefinedSymbolException =>
          // don't lose context
          throw createMissingEntityException(qid.fullName)
      }
  }
}

case class TypeResolver(
  typeMap: Map[String, FieldType] = Map(),
  constMap: Map[String, ConstDefinition] = Map(),
  serviceMap: Map[String, Service] = Map(),
  includeMap: Map[String, ResolvedDocument] = Map())
{
  lazy val fieldTypeResolver: EntityResolver[FieldType] =
    new EntityResolver(typeMap, includeMap, _.fieldTypeResolver) {
      def createMissingEntityException(name: String): Exception =
        new TypeNotFoundException(name)
    }
  lazy val serviceResolver: EntityResolver[Service] =
    new EntityResolver(serviceMap, includeMap, _.serviceResolver) {
      def createMissingEntityException(name: String): Exception =
        new UndefinedSymbolException(name)
    }
  lazy val constResolver: EntityResolver[ConstDefinition] =
    new EntityResolver(constMap, includeMap, _.constResolver) {
      def createMissingEntityException(name: String): Exception =
        new UndefinedConstantException(name)
    }


  /**
   * Returns a new TypeResolver with the given include mapping added.
   */
  def withMapping(inc: Include): TypeResolver = {
    val resolvedDocument = TypeResolver()(inc.document, Some(inc.prefix))
    copy(includeMap = includeMap + (inc.prefix.name -> resolvedDocument))
  }

  /**
   * Returns a new TypeResolver with the given type mapping added.
   */
  def withMapping(name: String, fieldType: FieldType): TypeResolver = {
    copy(typeMap = typeMap + (name -> fieldType))
  }

  /**
   * Returns a new TypeResolver with the given constant added.
   */
  def withMapping(const: ConstDefinition): TypeResolver = {
    copy(constMap = constMap + (const.sid.name -> const))
  }

  /**
   * Returns a new TypeResolver with the given service added.
   */
  def withMapping(service: Service): TypeResolver = {
    copy(serviceMap = serviceMap + (service.sid.name -> service))
  }

  /**
   * Resolves all types in the given document.
   */
  def apply(doc: Document, forcePrefix: Option[SimpleID] = None): ResolvedDocument = {
    var resolver = this
    val includes = doc.headers.collect { case i: Include => i }
    val defBuf = new ArrayBuffer[Definition](doc.defs.size)

    for (i <- includes) {
      resolver = resolver.withMapping(i)
    }

    for (d <- doc.defs) {
      val ResolvedDefinition(d2, r2) = resolver(d, forcePrefix)
      resolver = r2
      defBuf += d2
    }

    ResolvedDocument(doc.copy(defs = defBuf.toSeq), resolver)
  }

  /**
   * Resolves types in the given definition according to the current
   * typeMap, and then returns an updated TypeResolver with the new
   * definition bound, plus the resolved definition.
   */
  def apply(definition: Definition, forcePrefix: Option[SimpleID]): ResolvedDefinition
  = definition match {
    case d @ Typedef(sid, t) =>
      val resolved = apply(t)
      ResolvedDefinition(
        d.copy(fieldType = resolved),
        withMapping(sid.name, resolved))
    case s @ Struct(sid, _, fs, _) =>
      val resolved = s.copy(fields = fs.map(apply))
      ResolvedDefinition(
        resolved,
        withMapping(sid.name, StructType(resolved, forcePrefix)))
    case u @ Union(sid, _, fs, _) =>
      val resolved = u.copy(fields = fs.map(apply))
      ResolvedDefinition(
        resolved,
        withMapping(sid.name, StructType(resolved, forcePrefix)))
    case e @ Exception_(sid, _, fs, _) =>
      val resolved = e.copy(fields = fs.map(apply))
      ResolvedDefinition(
        resolved,
        // todo CSL-272: why no forcedPrefix is needed, unlike the Struct case?
        withMapping(sid.name, StructType(resolved)))
    case c @ ConstDefinition(_, t, v, _) =>
      val fieldType = apply(t)
      val resolved = c.copy(fieldType = fieldType, value = apply(v, fieldType))
      ResolvedDefinition(resolved, withMapping(resolved))
    case s @ Service(sid, p, fs, _) =>
      val resolved = s.copy(parent = p.map(apply), functions = fs.map(apply))
      ResolvedDefinition(resolved, withMapping(resolved))
    case e @ Enum(sid, _, _) =>
      // todo CSL-272: why no forcedPrefix is needed, unlike the Struct case?
      ResolvedDefinition(e, withMapping(sid.name, EnumType(e)))
    case s @ Senum(sid, _) =>
      ResolvedDefinition(s, withMapping(sid.name, TString))
    case d: EnumField => ResolvedDefinition(d, this)
    case d: FunctionArgs => ResolvedDefinition(d, this)
    case d: FunctionResult => ResolvedDefinition(d, this)
  }

  def apply(f: Function): Function = f match {
    case Function(_, _, t, as, ts, _) =>
      f.copy(funcType = apply(t), args = as.map(apply), throws = ts.map(apply))
  }

  def apply(f: Field): Field = {
    val fieldType = apply(f.fieldType)
    f.copy(
      fieldType = fieldType,
      default = f.default.map { const => apply(const, fieldType) })
  }

  def apply(t: FunctionType): FunctionType = t match {
    case Void => Void
    case OnewayVoid => OnewayVoid
    case t: FieldType => apply(t)
  }

  def apply(t: FieldType): FieldType = t match {
    case ReferenceType(id) => apply(id)
    case m @ MapType(k, v, _) => m.copy(keyType = apply(k), valueType = apply(v))
    case s @ SetType(e, _) => s.copy(eltType = apply(e))
    case l @ ListType(e, _) => l.copy(eltType = apply(e))
    case b: BaseType => b
    case e: EnumType => e
    case s: StructType => s
  }

  def apply(c: RHS, fieldType: FieldType): RHS = c match {
    // list values and map values look the same in Thrift, but different in Java and Scala
    // So we need type information in order to generated correct code.
    case l @ ListRHS(elems) =>
      fieldType match {
        case ListType(eltType, _) => l.copy(elems = elems map { e => apply(e, eltType) } )
        case SetType(eltType, _) => SetRHS(elems map { e => apply(e, eltType) } toSet)
        case _ => throw new TypeMismatchException("Expecting " + fieldType + ", found " + l)
      }
    case m @ MapRHS(elems) =>
      fieldType match {
        case MapType(keyType, valType, _) =>
          m.copy(elems = elems.map { case (k, v) => (apply(k, keyType), apply(v, valType)) })
        case _ => throw new TypeMismatchException("Expecting " + fieldType + ", found " + m)
      }
    case i @ IdRHS(id) => id match {
      case sid: SimpleID =>
        // When the rhs value is a simpleID, it can only be a constant
        // defined in the same file
        val const = constResolver(sid)
        if (const.fieldType == fieldType)
          const.value
        else
          throw new TypeMismatchException(sid.name)
      case qid @ QualifiedID(names) =>
        fieldType match {
          case EnumType(enum, _) =>
            val valueName = if (names.size == 2) {
              // id is enumName.valueName
              val enumName = names.head
              if (enumName != enum.sid.name) throw new UndefinedSymbolException(enumName)
              else names.last
            } else {
              // id is scopeName.enumName.valueName
              // todo CSL-272: is it possible to have nested scopes? eg: scope1.scope2.enumName.valueName
              val enumNameWithScope = QualifiedID(names.dropRight(1)).fullName
              if (fieldTypeResolver(Identifier(enumNameWithScope)) != fieldType)
                throw new TypeMismatchException(enumNameWithScope)
              else names.last
            }
            enum.values.find(_.sid.name == valueName) match {
              case None => throw new UndefinedSymbolException(qid.fullName)
              case Some(value) => EnumRHS(enum, value)
            }
          case t: BaseType =>
            val const = constResolver(qid)
            if (const.fieldType != fieldType) throw new TypeMismatchException(qid.fullName)
            else const.value
          case _ => throw new UndefinedSymbolException(qid.fullName)
        }
    }
    case _ => c
  }

  def apply(parent: ServiceParent): ServiceParent = {
    val parentID = parent.prefix match {
      case Some(p) => parent.sid.addScope(p)
      case None => parent.sid
    }
    parent.copy(service = Some(serviceResolver(parentID)))
  }

  def apply(id: Identifier): FieldType = id match {
    case sid: SimpleID => fieldTypeResolver(sid)
    case qid: QualifiedID => fieldTypeResolver(qid)
  }
}
