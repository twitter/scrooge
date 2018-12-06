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
import scala.util.parsing.input.{NoPosition, Positional}

class PositionalException(message: String, node: Positional)
    extends Exception(s"$message\n${node.pos.longString}")

case class TypeNotFoundException(name: String, node: Positional)
    extends PositionalException(name, node)
case class UndefinedConstantException(name: String, node: Positional)
    extends PositionalException(name, node)
case class UndefinedSymbolException(name: String, node: Positional)
    extends PositionalException(name, node)
case class TypeMismatchException(name: String, node: Positional)
    extends PositionalException(name, node)
case class QualifierNotFoundException(name: String, node: Positional)
    extends PositionalException(name, node)
case class DuplicatedIdentifierException(message: String, node: Positional)
    extends PositionalException(message, node)

case class ResolvedDocument(document: Document, resolver: TypeResolver) {

  /**
   * Given an ID, produce its FQN (e.g. a Java FQN) by appending the namespace.
   */
  def qualifySimpleID(
    sid: SimpleID,
    language: String,
    defaultNamespace: String,
    fallbackToJavaNamespace: Boolean = true
  ): Identifier = {
    val fallback = if (fallbackToJavaNamespace) document.namespace("java") else None
    val namespace =
      document.namespace(language).orElse(fallback).getOrElse(SimpleID(defaultNamespace))
    sid.addScope(namespace)
  }

  /**
   * Given a type, produce its FQN (e.g. a Java FQN) by appending the namespace.
   */
  def qualifyName(name: NamedType, language: String, defaultNamespace: String): Identifier = {
    name.scopePrefix match {
      case Some(filename) =>
        resolver.includeMap(filename.name).qualifySimpleID(name.sid, language, defaultNamespace)
      case None =>
        qualifySimpleID(name.sid, language, defaultNamespace)
    }
  }

  /**
   * Collect the chain of services extended by the given service.
   * Returns pairs (resolvedDoc, service) -- resolvedDoc contains service
   * and should be used to qualify types used by the service.
   */
  def collectParentServices(service: Service): Seq[(ResolvedDocument, Service)] = {
    service.parent match {
      case None => Nil
      case Some(ServiceParent(sid, None)) =>
        val parentService = resolver.resolveService(sid)
        (this, parentService) +: collectParentServices(parentService)
      case Some(ServiceParent(sid, Some(filename))) =>
        val doc: ResolvedDocument = resolver.includeMap(filename.name)
        val parentService = doc.resolver.resolveService(sid)
        (doc, parentService) +: doc.collectParentServices(parentService)
    }
  }

  /**
   * Collect and resolve services extended by the given service.
   *
   * @return a list of [[ResolvedService ResolvedServices]] that contain FQNs for the parent services.
   */
  def resolveParentServices(
    service: Service,
    namespaceLanguage: String,
    defaultNamespace: String
  ): Seq[ResolvedService] = {
    val resolvedServices: Seq[(ResolvedDocument, Service)] = collectParentServices(service)
    resolvedServices.map {
      case (rdoc, svc) =>
        ResolvedService(
          rdoc.qualifySimpleID(svc.sid.toTitleCase, namespaceLanguage, defaultNamespace),
          svc
        )
    }
  }
}

case class ResolvedService(serviceID: Identifier, service: Service)

case class ResolvedDefinition(definition: Definition, resolver: TypeResolver)

case class TypeResolver(
  typeMap: Map[String, FieldType] = Map.empty,
  constMap: Map[String, ConstDefinition] = Map.empty,
  serviceMap: Map[String, Service] = Map.empty,
  includeMap: Map[String, ResolvedDocument] = Map.empty,
  structsMap: Map[String, StructType] = Map.empty) {

  protected def getResolver(
    includePath: String,
    pos: Positional = new Positional {
      pos = NoPosition
    }
  ): TypeResolver = {
    includeMap
      .getOrElse(includePath, throw new QualifierNotFoundException(includePath, pos))
      .resolver
  }

  def resolveFieldType(id: Identifier): FieldType = id match {
    case sid: SimpleID =>
      val resolvedType = typeMap.get(sid.name) match {
        case Some(fieldType) => Some(fieldType)
        case None =>
          // Here we recurse so we can first resolve the types that are depended on by this type,
          // like an ersatz topological sort. However, this gets messy when we run into cycles,
          // like when two structs depend on each other. In order to avoid this, we add the entry
          // itself to the list of already resolved types when we recurse down, so that we don't
          // loop infinitely.
          structsMap.get(sid.name).flatMap { struct =>
            withType(sid.name, struct).typeMap.get(sid.name)
          }
      }
      resolvedType.getOrElse(throw new TypeNotFoundException(sid.name, id))
    case qid: QualifiedID => getResolver(qid.names.head, qid).resolveFieldType(qid.tail)
  }

  def resolveServiceParent(parent: ServiceParent): Service =
    parent.filename match {
      case None => resolveService(parent.sid)
      case Some(filename) => getResolver(filename.name).resolveService(parent.sid)
    }

  def resolveService(sid: SimpleID): Service =
    serviceMap.getOrElse(sid.name, throw new UndefinedSymbolException(sid.name, sid))

  def resolveConst(id: Identifier): (FieldType, RHS) = id match {
    case SimpleID(name, _) =>
      val const = constMap.getOrElse(name, throw new UndefinedConstantException(name, id))
      (const.fieldType, const.value)
    case qid: QualifiedID => getResolver(qid.names.head).resolveConst(qid.tail)
  }

  /**
   * Returns a new TypeResolver with the given include mapping added.
   */
  def withInclude(inc: Include): TypeResolver = {
    val resolver = TypeResolver()
    val resolvedDocument = resolver(inc.document, Some(inc.prefix))
    copy(includeMap = includeMap + (inc.prefix.name -> resolvedDocument))
  }

  /**
   * Returns a new TypeResolver with the given type mapping added.
   */
  def withType(name: String, fieldType: FieldType): TypeResolver = {
    copy(typeMap = typeMap + (name -> fieldType))
  }

  /**
   * Returns a new TypeResolver with the given constant added.
   */
  def withConst(const: ConstDefinition): TypeResolver = {
    copy(constMap = constMap + (const.sid.name -> const))
  }

  /**
   * Returns a new TypeResolver with the given service added.
   */
  def withService(service: Service): TypeResolver = {
    copy(serviceMap = serviceMap + (service.sid.name -> service))
  }

  def withStructsFrom(doc: Document, scopePrefix: Option[SimpleID]): TypeResolver = {
    val toAdd = doc.defs.flatMap {
      case s: Struct => Some(s.sid.name -> StructType(s, scopePrefix))
      case _ => None
    }.toMap

    copy(structsMap = structsMap ++ toAdd)
  }

  /**
   * Resolves all types in the given document.
   *
   * @param scopePrefix the scope of the document if the document is an include
   */
  def apply(doc: Document, scopePrefix: Option[SimpleID] = None): ResolvedDocument = {
    var resolver = this
    val includes = doc.headers.collect { case i: Include => i }
    val defBuf = new ArrayBuffer[Definition](doc.defs.size)

    for (i <- includes) {
      try {
        resolver = resolver.withInclude(i)
      } catch {
        case ex: Throwable =>
          throw new FileParseException(filename = i.filePath, cause = ex)
      }
    }

    resolver = resolver.withStructsFrom(doc, scopePrefix)

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
      case d: Typedef =>
        val resolved = apply(d.fieldType)
        ResolvedDefinition(d.copy(fieldType = resolved), withType(d.sid.name, resolved))
      case s: Struct =>
        // Do not allow Structs with the same name as a Typedef
        val resolver = if (typeMap.contains(s.sid.name)) {
          val fieldType: FieldType = typeMap(s.sid.name)
          if (fieldType != StructType(s, scopePrefix))
            throw new DuplicatedIdentifierException(
              s"Detected a duplicated identifier [${s.sid.name}] for differing types: Struct, ${typeMap(
                s.sid.name)}",
              s
            )
          else this // return the current TypeResolver as we've already resolved this type
        } else {
          // Add the current struct name to the scope to allow self referencing types
          // TODO: Enforce optional with self referenced field.
          // For now, we'll depend on the language compiler to error out in those cases.
          withType(s.sid.name, StructType(s, scopePrefix))
        }
        val resolved = s.copy(fields = s.fields.map(resolver.apply))
        ResolvedDefinition(resolved, withType(s.sid.name, StructType(resolved, scopePrefix)))
      case u: Union =>
        val resolved = u.copy(fields = u.fields.map(apply))
        ResolvedDefinition(resolved, withType(u.sid.name, StructType(resolved, scopePrefix)))
      case e: Exception_ =>
        val resolved = e.copy(fields = e.fields.map(apply))
        ResolvedDefinition(resolved, withType(e.sid.name, StructType(resolved, scopePrefix)))
      case c: ConstDefinition =>
        val fieldType = apply(c.fieldType)
        val resolved = c.copy(fieldType = fieldType, value = apply(c.value, fieldType))
        ResolvedDefinition(resolved, withConst(resolved))
      case s: Service =>
        // No need to modify Service, but check that we can resolve parent.
        s.parent.foreach { serviceParent =>
          resolveServiceParent(serviceParent)
        }
        val resolved = s.copy(functions = s.functions.map(apply))
        ResolvedDefinition(resolved, withService(resolved))
      case e: Enum =>
        ResolvedDefinition(e, withType(e.sid.name, EnumType(e, scopePrefix)))
      case s: Senum =>
        ResolvedDefinition(s, withType(s.sid.name, TString))
      case d: EnumField => ResolvedDefinition(d, this)
      case d: FunctionArgs => ResolvedDefinition(d, this)
      case d: FunctionResult => ResolvedDefinition(d, this)
    }
  }

  def apply(f: Function): Function = f match {
    case Function(_, _, t, as, ts, _, _) =>
      f.copy(funcType = apply(t), args = as.map(apply), throws = ts.map(apply))
  }

  def apply(f: Field): Field = {
    val fieldType = apply(f.fieldType)
    f.copy(fieldType = fieldType, default = f.default.map { const =>
      apply(const, fieldType)
    })
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
          val fieldMultiMap: Map[String, Seq[(String, RHS)]] = elems
            .collect {
              case (StringLiteral(fieldName), value) => (fieldName, value)
            }
            .groupBy { case (fieldName, _) => fieldName }

          val fieldMap: Map[String, RHS] = fieldMultiMap.collect {
            case (fieldName: String, values: Seq[(String, RHS)]) if values.length == 1 =>
              values.head
            case (fieldName: String, _: Seq[(String, RHS)]) =>
              throw new TypeMismatchException(
                s"Duplicate default values for $fieldName found for $fieldType",
                m
              )
            // Can't have 0 elements here because fieldMultiMap is built by groupBy.
          }

          structLike match {
            case u: Union =>
              val definedFields = u.fields.collect {
                case field if fieldMap.contains(field.sid.name) =>
                  (field, fieldMap(field.sid.name))
              }
              if (definedFields.isEmpty)
                throw new UndefinedConstantException(
                  s"Constant value missing for union ${u.originalName}",
                  m
                )
              if (definedFields.length > 1)
                throw new UndefinedConstantException(
                  s"Multiple constant values for union ${u.originalName}",
                  m
                )

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
                  throw new TypeMismatchException(s"Value required for $fieldName in $fieldType", m)
                }
              }
              StructRHS(sid = st.sid, elems = structMap.result())
          }
        case _ => throw new TypeMismatchException("Expecting " + fieldType + ", found " + m, m)
      }
    case i @ IdRHS(id) =>
      val (constFieldType, constRHS) = id match {
        case sid: SimpleID =>
          // When the rhs value is a simpleID, it can only be a constant
          // defined in the same file
          resolveConst(sid)
        case qid @ QualifiedID(names) =>
          fieldType match {
            case EnumType(enum, _) =>
              val resolvedFieldType = resolveFieldType(qid.qualifier)
              val value = enum.values
                .find(_.sid.name == names.last)
                .getOrElse(throw new UndefinedSymbolException(qid.fullName, qid))
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
    case _ => c
  }
}
