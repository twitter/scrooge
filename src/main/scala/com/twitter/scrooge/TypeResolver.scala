package com.twitter.scrooge

import AST._
import scala.collection.mutable.ArrayBuffer

class TypeNotFoundException(name: String) extends Exception(name)
class UndefinedSymbolException(name: String) extends Exception(name)

case class ResolvedDocument(document: Document, resolver: TypeResolver)
case class ResolvedDefinition(definition: Definition, resolver: TypeResolver)

case class TypeResolver(
  typeMap: Map[String,FieldType] = Map(),
  includeMap: Map[String,ResolvedDocument] = Map())
{
  /**
   * Resolves all types in the given document.
   */
  def resolve(doc: Document): ResolvedDocument = {
    var resolver = this
    val includes = doc.headers.collect { case i: Include => i }
    val defBuf = new ArrayBuffer[Definition](doc.defs.size)

    for (i <- includes) {
      resolver = resolver.include(i)
    }

    for (d <- doc.defs) {
      val ResolvedDefinition(d2, r2) = resolver.resolve(d)
      resolver = r2
      defBuf += d2
    }

    ResolvedDocument(doc.copy(defs = defBuf.toSeq), resolver)
  }

  /**
   * Returns a new TypeResolver with the given include mapping added.
   */
  def include(inc: Include): TypeResolver = {
    val resolvedDocument = TypeResolver().resolve(inc.document)
    copy(includeMap = includeMap + (inc.prefix -> resolvedDocument))
  }

  /**
   * Resolves types in the given definition according to the current
   * typeMap, and then returns an updated TypeResolver with the new
   * definition bound, plus the resolved definition.
   */
  def resolve(definition: Definition): ResolvedDefinition = {
    apply(definition) match {
      case d @ Typedef(name, t) => ResolvedDefinition(d, define(name, t))
      case e @ Enum(name, _) => ResolvedDefinition(e, define(name, EnumType(e)))
      case s @ Struct(name, _) => ResolvedDefinition(s, define(name, StructType(s)))
      case e @ Exception_(name, _) => ResolvedDefinition(e, define(e.name, StructType(e)))
      case d => ResolvedDefinition(d, this)
    }
  }

  /**
   * Returns a new TypeResolver with the given type mapping added.
   */
  def define(name: String, `type`: FieldType): TypeResolver = {
    copy(typeMap = typeMap + (name -> `type`))
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
    case ReferenceType(name) => apply(name)
    case m @ MapType(k, v, _) => m.copy(keyType = apply(k), valueType = apply(v))
    case s @ SetType(e, _) => s.copy(eltType = apply(e))
    case l @ ListType(e, _) => l.copy(eltType = apply(e))
    case _ => t
  }

  def apply(name: String): FieldType = {
    name.indexOf('.') match {
      case -1 => typeMap.get(name).getOrElse(throw new TypeNotFoundException(name))
      case dot =>
        val prefix = name.substring(0, dot)
        val suffix = name.substring(dot + 1)
        val include = includeMap.get(prefix).getOrElse(throw new UndefinedSymbolException(name))

        try {
          include.resolver(suffix)
        } catch {
          case ex: TypeNotFoundException =>
            // don't lose context
            throw new TypeNotFoundException(name)
        }
    }
  }
}
