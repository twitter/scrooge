package com.twitter.scrooge
package scalagen

import AST._
import com.twitter.handlebar.{Dictionary, Handlebar}

trait ServiceTemplate extends Generator { self: ScalaGenerator =>
  import Dictionary._

  def toDictionary(function: Function, async: Boolean): Dictionary = {
    val throwsDictionaries = function.throws map { t =>
      Dictionary("scalaType" -> v(scalaType(t.`type`)))
    }
    val baseReturnType = scalaType(function.`type`)
    val returnType = if (async) "Future[" + baseReturnType + "]" else baseReturnType
    Dictionary(
      "throws" -> v(throwsDictionaries),
      "name" -> v(function.localName),
      "scalaType" -> v(returnType),
      "fieldArgs" -> v(fieldArgs(function.args))
    )
  }

  lazy val functionTemplate = templates("function") { f: Function => toDictionary(f, false) }
  lazy val futureFunctionTemplate = templates("function") { f: Function => toDictionary(f, true) }

  def serviceFunctionArgsStruct(f: Function): Struct = {
    Struct(f.localName + "_args", f.args)
  }

  def serviceFunctionResultStruct(f: Function): Struct = {
    val throws = f.throws map { _.copy(requiredness = Requiredness.Optional) }
    val success = f.`type` match {
      case Void => Nil
      case fieldType: FieldType =>
        Seq(Field(0, "success", fieldType, None, Requiredness.Optional))
    }
    Struct(f.localName + "_result", success ++ throws)
  }

  lazy val finagleClientFunctionTemplate = templates("finagleClientFunction") { f: Function =>
    val resultUnwrapper = {
      val exceptions = if (f.throws.isEmpty) "" else
         f.throws.map { "result." + _.name } mkString("(", " orElse ", ").map(Future.exception) getOrElse ")
      val result = if (f.`type` eq AST.Void) "Future.Done" else
         "{ result.success.map(Future.value) getOrElse missingResult(\"" + f.name + "\") }"
      exceptions + result
    }
    Dictionary(
      "functionDecl" -> v(futureFunctionTemplate(f)),
      "name" -> v(f.name),
      "localName" -> v(f.localName),
      "argNames" -> v(f.args.map(_.name).mkString(", ")),
      "resultUnwrapper" -> v(resultUnwrapper)
    )
  }

  lazy val finagleClientTemplate = templates("finagleClient") { s: Service =>
    val functionDictionaries = s.functions map { f =>
      Dictionary("function" -> v(finagleClientFunctionTemplate(f).indent() + "\n"))
    }
    val parentName = s.parent.flatMap(_.service).map(_.name)
    Dictionary(
      "override" -> v(if (parentName.nonEmpty) "override " else ""),
      "extends" -> v(parentName.map { _ + ".FinagledClient(service, protocolFactory)" }.getOrElse("FinagleThriftClient")),
      "functions" -> v(functionDictionaries)
    )
  }

  lazy val finagleServiceFunctionTemplate = templates("finagleServiceFunction") { f: Function =>
    val exceptionDictionaries = f.throws map { t =>
      Dictionary(
        "exceptionType" -> v(scalaType(t.`type`)),
        "fieldName" -> v(t.name)
      )
    }
    Dictionary(
      "name" -> v(f.name),
      "localName" -> v(f.localName),
      "argNames" -> v(f.args map { "args." + _.name } mkString(", ")),
      "scalaType" -> v(scalaType(f.`type`)),
      "resultNamedArg" -> v(if (f.`type` ne Void) "success = Some(value)" else ""),
      "exceptions" -> v(exceptionDictionaries)
    )
  }

  lazy val finagleServiceTemplate = templates("finagleService") { s: Service =>
    val functionDictionaries = s.functions map { f =>
      Dictionary(
        "function" -> v(finagleServiceFunctionTemplate(f).indent() + "\n")
      )
    }
    val parentName = s.parent.flatMap(_.service).map(_.name)
    Dictionary(
      "override" -> v(if (parentName.nonEmpty) "override " else ""),
      "extends" -> v(parentName.map { _ + ".FinagledService(iface, protocolFactory)" }.getOrElse("FinagleThriftService")),
      "functions" -> v(functionDictionaries)
    )
  }

  lazy val ostrichServiceTemplate = templates("ostrichService") { s: Service => Dictionary() }

  lazy val serviceTemplate = templates("service") { s: ScalaService =>
    val service = s.service
    val syncFunctions = service.functions.map(functionTemplate(_).indent(2)).mkString("\n\n")
    val asyncFunctions = service.functions.map(futureFunctionTemplate(_).indent(2)).mkString("\n\n")
    val functionStructs = service.functions flatMap { f =>
      Seq(serviceFunctionArgsStruct(f), serviceFunctionResultStruct(f))
    } map { structTemplate(_).indent } mkString("", "\n\n", "\n")
    val parentName = service.parent.flatMap(_.service).map(_.name)
    Dictionary(
      "name" -> v(service.name),
      "syncExtends" -> v(parentName.map { "extends " + _ + ".Iface " }.getOrElse("")),
      "asyncExtends" -> v(parentName.map { "extends " + _ + ".FutureIface " }.getOrElse("")),
      "syncFunctions" -> v(syncFunctions),
      "asyncFunctions" -> v(asyncFunctions),
      "functionStructs" -> v(functionStructs),
      "finagleClient" -> v(
        if (s.options contains WithFinagleClient) (finagleClientTemplate(service).indent + "\n") else ""
      ),
      "finagleService" -> v(
        if (s.options contains WithFinagleService) (finagleServiceTemplate(service).indent + "\n") else ""
      ),
      "ostrichServer" -> v(
        if (s.options contains WithOstrichServer) (ostrichServiceTemplate(service).indent + "\n") else ""
      )
    )
  }
}
