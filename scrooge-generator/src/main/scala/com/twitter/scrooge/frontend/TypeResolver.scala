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
import scala.util.parsing.input.Positional

class PositionalException(message: String, node: Positional)
  extends Exception(s"$message\n${node.pos.longString}")

case class TypeNotFoundException(name: String, node: Positional) extends PositionalException(name, node)
case class UndefinedConstantException(name: String, node: Positional) extends PositionalException(name, node)
case class UndefinedSymbolException(name: String, node: Positional) extends PositionalException(name, node)
case class TypeMismatchException(name: String, node: Positional) extends PositionalException(name, node)
case class QualifierNotFoundException(name: String, node: Positional) extends PositionalException(name, node)

case class ResolvedDocument(document: Document, resolver: TypeResolver)
case class ResolvedDefinition(definition: Definition, resolver: TypeResolver)

case class TypeResolver(
    typeMap: Map[String, FieldType] = Map.empty,
    constMap: Map[String, ConstDefinition] = Map.empty,
    serviceMap: Map[String, Service] = Map.empty,
    includeMap: Map[String, ResolvedDocument] = Map.empty) {

  def getResolver(qid: QualifiedID) = {
    includeMap.get(qid.names.head).getOrElse(throw new QualifierNotFoundException(qid.fullName, qid)).resolver
  }

  def resolveFieldType(id: Identifier): FieldType = id match {
    case SimpleID(name, _) => typeMap.get(name).getOrElse(throw new TypeNotFoundException(name, id))
    case qid: QualifiedID => getResolver(qid).resolveFieldType(qid.tail)
  }

  def resolveService(id: Identifier): Service = id match {
    case SimpleID(name, _) => serviceMap.get(name).getOrElse(throw new UndefinedSymbolException(name, id))
    case qid: QualifiedID => getResolver(qid).resolveService(qid.tail)
  }

  def resolveConst(id: Identifier): (FieldType, RHS) = id match {
    case SimpleID(name, _) =>
      val const = constMap.get(name).getOrElse(throw new UndefinedConstantException(name, id))
      (const.fieldType, const.value)
    case qid: QualifiedID => getResolver(qid).resolveConst(qid.tail)
  }

  /**
   * Returns a new TypeResolver with the given include mapping added.
   */
  def withMapping(inc: Include): TypeResolver = {
    val resolver = TypeResolver()
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
      try {
        resolver = resolver.withMapping(i)
      } catch {
        case ex: Throwable =>
          throw new FileParseException(filename = i.filePath, cause = ex)
      }
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
      case e @ Enum(sid, _, _, _) =>
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
        case ListType(eltType, _) => l.copy(elems = elems.map(e => apply(e, eltType)))
        case SetType(eltType, _) => SetRHS(elems.map(e => apply(e, eltType)).toSet)
        case _ => throw new TypeMismatchException("Expecting " + fieldType + ", found " + l, c)
      }
    case m @ MapRHS(elems) =>
      fieldType match {
        case MapType(keyType, valType, _) =>
          m.copy(elems = elems.map { case (k, v) => (apply(k, keyType), apply(v, valType)) })
        case st @ StructType(structLike: StructLike, _) =>
          val fieldMultiMap: Map[String, Seq[(String, RHS)]] = elems.collect {
            case (StringLiteral(fieldName), value) => (fieldName, value)
          }.groupBy { case (fieldName, _) => fieldName }

          val fieldMap: Map[String, RHS] = fieldMultiMap.collect {
            case (fieldName: String, values: Seq[(String, RHS)]) if values.length == 1 =>
              values.head
            case (fieldName: String, _: Seq[(String, RHS)]) =>
              throw new TypeMismatchException(s"Duplicate default values for ${fieldName} found for $fieldType", m)
            // Can't have 0 elements here because fieldMultiMap is built by groupBy.
          }

          structLike match {
            case u: Union =>
              val definedFields = u.fields.collect {
                case field if fieldMap.contains(field.sid.name) =>
                  (field, fieldMap(field.sid.name))
              }
              if (definedFields.length == 0)
                throw new UndefinedConstantException(s"Constant value missing for union ${u.originalName}", m)
              if (definedFields.length > 1)
                throw new UndefinedConstantException(s"Multiple constant values for union ${u.originalName}", m)

              val (field, rhs) = definedFields.head
              val resolvedRhs = apply(rhs, field.fieldType)
              UnionRHS(sid = st.sid, field = field, initializer = resolvedRhs)

            case struct: StructLike =>
              val structMap = Map.newBuilder[Field, RHS]
              struct.fields.foreach { field =>
                val fieldName = field.sid.name
                if (fieldMap.contains(fieldName)) {
                  val resolvedRhs = apply(fieldMap(fieldName), field.fieldType)
                  structMap += field -> resolvedRhs
                } else if (!field.requiredness.isOptional && field.default.isEmpty) {
                  throw new TypeMismatchException(s"Value required for ${fieldName} in $fieldType", m)
                }
              }
              StructRHS(sid = st.sid, elems = structMap.result())
          }
        case _ => throw new TypeMismatchException("Expecting " + fieldType + ", found " + m, m)
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
                throw new UndefinedSymbolException(qid.fullName, qid))
              (resolvedFieldType, EnumRHS(enum, value))
            case t => resolveConst(qid)
          }
      }
      if (constFieldType != fieldType)
        throw new TypeMismatchException(
          s"Type mismatch: Expecting $fieldType, found ${id.fullName}: $constFieldType",
          id
        )
      constRHS
    }
    case _ => c
  }

  def apply(parent: ServiceParent): ServiceParent = {
    parent.filename match {
      case Some(filename) =>
        val parentID = parent.sid.addScope(filename)
        parent.copy(service = Some(resolveService(parentID)),
          doc = includeMap.get(filename.name))
      case None =>
        parent.copy(service = Some(resolveService(parent.sid)))
    }
  }
}
