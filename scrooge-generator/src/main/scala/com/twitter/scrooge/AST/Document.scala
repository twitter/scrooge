package com.twitter.scrooge.ast

case class Document(headers: Seq[Header], defs: Seq[Definition]) extends DocumentNode {
  def namespace(language: String): Option[Identifier] = {
    headers
      .collectFirst {
        // first try to find language specific namespace scope
        case Namespace(l, x) if l == language => x
      }.orElse(headers.collectFirst {
        // then see if universal namespace scope is defined
        case Namespace(l, x) if l == "*" => x
      })
  }

  def mapNamespaces(namespaceMap: Map[String, String]): Document = {
    copy(
      headers = headers.map {
        case header @ Namespace(_, ns) =>
          namespaceMap
            .get(ns.fullName).map { newNs =>
              header.copy(id = Identifier(newNs))
            }.getOrElse(header)
        case include @ Include(_, doc) =>
          include.copy(document = doc.mapNamespaces(namespaceMap))
        case header => header
      }
    )
  }

  def validator: Option[Identifier] = headers.collectFirst { case Validator(path) => path }

  def consts: Seq[ConstDefinition] = defs.collect { case c: ConstDefinition => c }
  def enums: Seq[Enum] = defs.collect { case e: Enum => e }
  def structs: Seq[StructLike] = defs.collect { case s: StructLike => s }
  def services: Seq[Service] = defs.collect { case s: Service => s }
}
