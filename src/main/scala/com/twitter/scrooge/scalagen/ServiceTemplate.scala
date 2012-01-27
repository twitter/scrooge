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
package scalagen

import AST._
import com.twitter.handlebar.{Dictionary, Handlebar}

trait ServiceTemplate extends Generator { self: ScalaGenerator =>
  import Dictionary._

  def toDictionary(function: Function, async: Boolean): Dictionary = {
    val throwsDictionaries = if (async) Nil else function.throws map { t =>
      Dictionary("scalaType" -> v(scalaType(t.`type`)))
    }
    val baseReturnType = scalaType(function.`type`)
    val returnType = if (async) "Future[" + baseReturnType + "]" else baseReturnType
    Dictionary(
      "throws" -> v(throwsDictionaries),
      "name" -> v(function.localName),
      "scalaType" -> v(returnType),
      "fieldParams" -> v(fieldParams(function.args))
    )
  }

  lazy val functionTemplate = templates("function").generate { f: Function => toDictionary(f, false) }
  lazy val futureFunctionTemplate = templates("function").generate { f: Function => toDictionary(f, true) }

  def serviceFunctionArgsStruct(f: Function): FunctionArgs = {
    FunctionArgs(f.localName + "_args", f.args)
  }

  def serviceFunctionResultStruct(f: Function): FunctionResult = {
    val throws = f.throws map { _.copy(requiredness = Requiredness.Optional) }
    val success = f.`type` match {
      case Void => Nil
      case fieldType: FieldType =>
        Seq(Field(0, "success", fieldType, None, Requiredness.Optional))
    }
    FunctionResult(f.localName + "_result", success ++ throws)
  }

  lazy val finagleClientFunctionTemplate = templates("finagleClientFunction").generate { f: Function =>
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

  lazy val finagleClientTemplate = templates("finagleClient").generate { s: Service =>
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

  lazy val finagleServiceFunctionTemplate = templates("finagleServiceFunction").generate { f: Function =>
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

  lazy val finagleServiceTemplate = templates("finagleService").generate { s: Service =>
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

  lazy val ostrichServiceTemplate = templates("ostrichService").generate { s: Service => Dictionary() }

  lazy val serviceTemplate = templates("service").generate { s: ScalaService =>
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
