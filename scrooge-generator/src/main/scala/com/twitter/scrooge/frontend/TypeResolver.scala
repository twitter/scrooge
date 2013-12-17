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

package com.twitter.scrooge.frontend

import com.twitter.scrooge.ast._
import scala.collection.mutable.ArrayBuffer

class TypeNotFoundException(name: String) extends Exception(name)
class UndefinedConstantException(name: String) extends Exception(name)
class UndefinedSymbolException(name: String) extends Exception(name)
class TypeMismatchException(name: String) extends Exception(name)
class QualifierNotFoundException(name: String) extends Exception(name)

case class ResolvedDocument(document: Document, resolver: TypeResolver)
case class ResolvedDefinition(definition: Definition, resolver: TypeResolver)

case class TypeResolver(
  typeMap: Map[String, FieldType] = Map(),
  constMap: Map[String, ConstDefinition] = Map(),
  serviceMap: Map[String, Service] = Map(),
  includeMap: Map[String, ResolvedDocument] = Map(),
  allowStructRHS: Boolean = false) {

  def getResolver(qid: QualifiedID) = {
    includeMap.get(qid.names.head).getOrElse(throw new QualifierNotFoundException(qid.fullName)).resolver
  }

  def resolveFieldType(id: Identifier): FieldType = id match {
    case SimpleID(name) => typeMap.get(name).getOrElse(throw new TypeNotFoundException(name))
    case qid: QualifiedID => getResolver(qid).resolveFieldType(qid.tail)
  }

  def resolveService(id: Identifier): Service = id match {
    case SimpleID(name) => serviceMap.get(name).getOrElse(throw new UndefinedSymbolException(name))
    case qid: QualifiedID => getResolver(qid).resolveService(qid.tail)
  }

  def resolveConst(id: Identifier): (FieldType, RHS) = id match {
    case SimpleID(name) =>
      val const = constMap.get(name).getOrElse(throw new UndefinedConstantException(name))
      (const.fieldType, const.value)
    case qid: QualifiedID => getResolver(qid).resolveConst(qid.tail)
  }

  /**
   * Returns a new TypeResolver with the given include mapping added.
   */
  def withMapping(inc: Include): TypeResolver = {
    val resolver = TypeResolver(allowStructRHS = allowStructRHS)
    val resolvedDocument = resolver(inc.document, Some(inc.prefix))
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
   * @param scopePrefix the scope of the document if the document is an include
   */
  def apply(doc: Document, scopePrefix: Option[SimpleID] = None): ResolvedDocument = {
    var resolver = this
    val includes = doc.headers.collect { case i: Include => i }
    val defBuf = new ArrayBuffer[Definition](doc.defs.size)

    for (i <- includes) {
      resolver = resolver.withMapping(i)
    }

    for (d <- doc.defs) {
      val ResolvedDefinition(d2, r2) = resolver(d, scopePrefix)
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
  def apply(definition: Definition, scopePrefix: Option[SimpleID]): ResolvedDefinition = {
    definition match {
      case d @ Typedef(sid, t, _) =>
        val resolved = apply(t)
        ResolvedDefinition(
          d.copy(fieldType = resolved),
          withMapping(sid.name, resolved))
      case s @ Struct(sid, _, fs, _, _) =>
        val resolved = s.copy(fields = fs.map(apply))
        ResolvedDefinition(
          resolved,
          withMapping(sid.name, StructType(resolved, scopePrefix)))
      case u @ Union(sid, _, fs, _, _) =>
        val resolved = u.copy(fields = fs.map(apply))
        ResolvedDefinition(
          resolved,
          withMapping(sid.name, StructType(resolved, scopePrefix)))
      case e @ Exception_(sid, _, fs, _) =>
        val resolved = e.copy(fields = fs.map(apply))
        ResolvedDefinition(
          resolved,
          withMapping(sid.name, StructType(resolved, scopePrefix)))
      case c @ ConstDefinition(_, t, v, _) =>
        val fieldType = apply(t)
        val resolved = c.copy(fieldType = fieldType, value = apply(v, fieldType))
        ResolvedDefinition(resolved, withMapping(resolved))
      case s @ Service(sid, p, fs, _) =>
        val resolved = s.copy(parent = p.map(apply), functions = fs.map(apply))
        ResolvedDefinition(resolved, withMapping(resolved))
      case e @ Enum(sid, _, _) =>
        ResolvedDefinition(e, withMapping(sid.name, EnumType(e, scopePrefix)))
      case s @ Senum(sid, _) =>
        ResolvedDefinition(s, withMapping(sid.name, TString))
      case d: EnumField => ResolvedDefinition(d, this)
      case d: FunctionArgs => ResolvedDefinition(d, this)
      case d: FunctionResult => ResolvedDefinition(d, this)
    }
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
    case ReferenceType(id) => resolveFieldType(id)
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
        case StructType(s, _) if allowStructRHS =>
          val structMap = Map.newBuilder[SimpleID, RHS]
          s.fields.foreach { f =>
            val filtered = elems.filter {
              case (StringLiteral(fieldName), _) => fieldName == f.sid.name
              case _ => false
            }
            if (filtered.size == 1) {
              val (k, v) = filtered.head
              structMap += f.sid -> apply(v, f.fieldType)
            } else if (filtered.size > 1) {
              throw new TypeMismatchException("Duplicate default values for " + f.sid.name + " found for " + fieldType)
            } else if (!f.requiredness.isOptional && f.default.isEmpty) {
              throw new TypeMismatchException("Default value for " + f.sid.name + " needed for " + fieldType)
            }
          }
          StructRHS(elems = structMap.result())
        case _ => throw new TypeMismatchException("Expecting " + fieldType + ", found " + m)
      }
    case i @ IdRHS(id) => {
      val (constFieldType, constRHS) = id match {
        case sid: SimpleID =>
          // When the rhs value is a simpleID, it can only be a constant
          // defined in the same file
          resolveConst(sid)
        case qid @ QualifiedID(names) =>
          fieldType match {
            case EnumType(enum, _) =>
              val resolvedFieldType = resolveFieldType(qid.qualifier)
              val value = enum.values.find(_.sid.name == names.last).getOrElse(
                throw new UndefinedSymbolException(qid.fullName))
              (resolvedFieldType, EnumRHS(enum, value))
            case t => resolveConst(qid)
          }
      }
      if (constFieldType != fieldType) throw new TypeMismatchException(id.fullName)
      constRHS
    }
    case _ => c
  }

  def apply(parent: ServiceParent): ServiceParent = {
    val parentID = parent.prefix match {
      case Some(p) => parent.sid.addScope(p)
      case None => parent.sid
    }
    parent.copy(service = Some(resolveService(parentID)))
  }
}
