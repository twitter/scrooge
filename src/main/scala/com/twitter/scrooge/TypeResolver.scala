package com.twitter.scrooge

import AST._

class TypeNotFoundException(name: String) extends Exception(name)

class TypeResolver(typeMap: Map[String,FieldType] = Map()) {
  /**
   * Returns a copy of Document, but with all ReferenceTypes applyd
   * to the referred-to types.  Throws a TypeNotFoundException if a
   * an undefined type reference is encounted.
   */
  def apply(doc: Document): Document = {
    doc.copy(defs = applyAll(doc.defs.toList))
  }

  /**
   * Returns a new TypeResolver with the given type mapping added.
   */
  def define(name: String, `type`: FieldType) = {
    new TypeResolver(typeMap + (name -> `type`))
  }

  /**
   * Resolves types in the given definition according to the current
   * typeMap, and then returns an updated TypeResolver with the new
   * definition bound, plus the resolved definition.
   */
  def define(definition: Definition): (TypeResolver, Definition) = {
    apply(definition) match {
      case d @ Typedef(name, t) => (define(name, t), d)
      case e @ Enum(name, _) => (define(name, EnumType(e)), e)
      case s @ Struct(name, _) => (define(name, StructType(s)), s)
      case e @ Exception_(name, _) => (define(e.name, StructType(e)), e)
      case d => (this, d)
    }
  }

  def applyAll(defs: List[Definition]): List[Definition] = {
    defs match {
      case Nil => Nil
      case head :: tail => define(head) match {
        case (typeCtx, applydDef) => applydDef :: typeCtx.applyAll(tail)
      }
    }
  }

  def apply(definition: Definition): Definition = {
    definition match {
      case d @ Typedef(name, t) => d.copy(`type` = apply(t))
      case s @ Struct(_, fs) => s.copy(fields = fs.map(apply))
      case e @ Exception_(_, fs) => e.copy(fields = fs.map(apply))
      case c @ Const(_, t, _) => c.copy(`type` = apply(t))
      case s @ Service(_, _, fs) => s.copy(functions = fs.map(apply))
      case d => d
    }
  }

  def apply(f: Function): Function = f match {
    case Function(_, t, as, _, ts) =>
      f.copy(`type` = apply(t), args = as.map(apply), throws = ts.map(apply))
  }

  def apply(f: Field): Field = f.copy(`type` = apply(f.`type`))

  def apply(t: FunctionType): FunctionType = t match {
    case Void => Void
    case t: FieldType => apply(t)
  }

  def apply(t: FieldType): FieldType = t match {
    case ReferenceType(name) => typeMap.get(name).getOrElse(throw new TypeNotFoundException(name))
    case m @ MapType(k, v, _) => m.copy(keyType = apply(k), valueType = apply(v))
    case s @ SetType(e, _) => s.copy(eltType = apply(e))
    case l @ ListType(e, _) => l.copy(eltType = apply(e))
    case _ => t
  }
}
