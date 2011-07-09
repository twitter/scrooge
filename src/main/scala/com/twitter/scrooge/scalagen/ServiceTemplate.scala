package com.twitter.scrooge
package scalagen

import AST._

object ServiceTemplate extends ScalaTemplate {
  val functionThrowsTemplate = template[Field]("@throws(classOf[{{ scalaType(`type`) }}])")

  val functionArgTemplate = template[Field]("{{name}}: {{ scalaFieldType(self) }}")

  val functionDeclarationTemplate = template[Function](
"""def {{name}}({{ args.map { a => functionArgTemplate(a, scope) }.mkString(", ") }}): {{scalaType(`type`)}}""")

  val futureFunctionDeclarationTemplate = template[Function](
"""def {{name}}({{ args.map { a => functionArgTemplate(a, scope) }.mkString(", ") }}): Future[{{scalaType(`type`)}}]""")

  val functionTemplate = template[Function](
"""{{ throws.map { t => functionThrowsTemplate(t, scope) + "\n" }.mkString }}{{ functionDeclarationTemplate(self, scope) }}""")

  val futureFunctionTemplate = template[Function](
"""{{ throws.map { t => functionThrowsTemplate(t, scope) + "\n" }.mkString }}{{ futureFunctionDeclarationTemplate(self, scope) }}""")

  val serviceTemplate = template[Service](
"""object {{name}} {
  trait Iface {{ parent.map { "extends " + _ }.getOrElse("") }}{
{{ functions.map { f => functionTemplate(f, scope).indent(2) }.mkString("\n") }}
  }

  trait FutureIface {{ parent.map { "extends " + _ }.getOrElse("") }}{
{{ functions.map { f => futureFunctionTemplate(f, scope).indent(2) }.mkString("\n") }}
  }

{{
functions.map { f =>
  // generate a Struct for each function's args & retval
  structTemplate(AST.Struct(f.name + "_args", f.args), scope) + "\n" +
    structTemplate(AST.Struct(f.name + "_result", Array(AST.Field(0, "success", f.`type`.asInstanceOf[AST.FieldType], None, AST.Requiredness.Required))), scope)
}.mkString("\n").indent
}}
}
""")
}
