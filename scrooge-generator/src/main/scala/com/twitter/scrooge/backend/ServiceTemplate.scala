package com.twitter.scrooge.backend

/**
 * Copyright 2011 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.twitter.scrooge.ast._
import com.twitter.scrooge.mustache.Dictionary
import com.twitter.scrooge.mustache.Dictionary._

trait ServiceTemplate {
  self: Generator =>
  def toDictionary(function: Function, async: Boolean): Dictionary = {
    val hasThrows = !async && function.throws.size > 0
    val throwsDictionaries =
      if (hasThrows) {
        function.throws map {
          t =>
            Dictionary("typeName" -> genType(t.fieldType))
        }
      } else {
        Nil
      }
    Dictionary(
      "async" -> v(async),
      "docstring" -> codify(function.docstring.getOrElse("")),
      "hasThrows" -> v(hasThrows),
      "throws" -> v(throwsDictionaries),
      "funcName" -> genID(function.funcName.toCamelCase),
      "typeName" -> genType(function.funcType),
      "fieldParams" -> genFieldParams(function.args)
    )
  }

  def serviceFunctionArgsStruct(f: Function): FunctionArgs = {
    FunctionArgs(f.funcName.append("_args"), f.args)
  }

  def serviceFunctionResultStruct(f: Function): FunctionResult = {
    val throws = f.throws map {
      _.copy(requiredness = Requiredness.Optional)
    }
    val success = f.funcType match {
      case Void => Nil
      case fieldType: FieldType =>
        Seq(Field(0, SimpleID("success"), fieldType, None, Requiredness.Optional))
    }
    FunctionResult(f.funcName.append("_result"), success ++ throws)
  }

  def finagleClient(s: Service) = {
    val parentName = s.parent.flatMap(_.service).map(_.sid.name)
    Dictionary(
      "hasParent" -> v(parentName.nonEmpty),
      "parent" -> codify(parentName map {
        _ + ".FinagledClient"
      } getOrElse ""),
      "functions" -> v(s.functions.map {
        f =>
          Dictionary(
            "header" -> v(templates("function")),
            "headerInfo" -> v(toDictionary(f, true)),
            "clientFuncName" -> genID(f.funcName.toCamelCase),
            "__stats_name" -> genID(f.funcName.toCamelCase.prepend("__stats_")),
            "type" -> genType(f.funcType),
            "void" -> v(f.funcType eq Void),
            "ArgsStruct" -> genID(f.funcName.append("Args").toTitleCase),
            "ResultStruct" -> genID(f.funcName.append("Result").toTitleCase),
            "argNames" -> {
              val code = f.args.map { field => genID(field.sid).toData }.mkString(", ")
              codify(code)
            },
            "hasThrows" -> v(f.throws.size > 0),
            "throws" -> v(f.throws.map {
              thro => Dictionary("throwName" -> genID(thro.sid))
            })
          )
      }),
      "function" -> v(templates("finagleClientFunction"))
    )
  }

  def finagleService(s: Service) = {
    val parentID = s.parent.flatMap(_.service).map(_.sid)
    Dictionary(
      "hasParent" -> v(parentID.nonEmpty),
      "parent" -> parentID.map { id =>
        codify(genID(id).toData + ".FinagledService")
      }.getOrElse(genBaseFinagleService),
      "function" -> v(templates("finagleServiceFunction")),
      "functions" -> v(s.functions map {
        f =>
          Dictionary(
            "serviceFuncName" -> genID(f.funcName.toCamelCase),
            "ArgsStruct" -> genID(f.funcName.append("Args").toTitleCase),
            "ResultStruct" -> genID(f.funcName.append("Result").toTitleCase),
            "argNames" ->
              codify(f.args map { field =>
                "args." + genID(field.sid).toData
              } mkString (", ")),
            "typeName" -> genType(f.funcType),
            "isVoid" -> v(f.funcType eq Void),
            "resultNamedArg" -> codify(if (f.funcType ne Void) "success = Some(value)" else ""),
            "exceptions" -> v(f.throws map {
              t =>
                Dictionary(
                  "exceptionType" -> genType(t.fieldType),
                  "fieldName" -> genID(t.sid)
                )
            })
          )
      })
    )
  }

  def ostrichService(s: Service) = Dictionary()

  def serviceDict(
    s: JavaService,
    namespace: Identifier,
    includes: Seq[Include],
    serviceOptions: Set[ServiceOption]
  ) = {
    val service = s.service

    val parentID = service.parent.flatMap(_.service).map(_.sid)
    Dictionary(
      "function" -> v(templates("function")),
      "package" -> genID(namespace),
      "ServiceName" -> genID(service.sid.toTitleCase),
      "docstring" -> codify(service.docstring.getOrElse("")),
      "syncExtends" -> codify(parentID.map { id =>
        "extends " + genID(id).toData + ".Iface "
      }.getOrElse("")),
      "asyncExtends" -> codify(parentID.map { id =>
        "extends " + genID(id) + ".FutureIface "
      }.getOrElse("")),
      "syncFunctions" -> v(service.functions.map {
        f => toDictionary(f, false)
      }),
      "asyncFunctions" -> v(service.functions.map {
        f => toDictionary(f, true)
      }),
      "struct" -> v(templates("struct")),
      "structs" -> v(
        service.functions flatMap {
          f =>
            Seq(serviceFunctionArgsStruct(f), serviceFunctionResultStruct(f))
        } map {
          struct =>
            structDict(struct, None, includes, serviceOptions)
        }),
      "finagleClient" -> v(templates("finagleClient")),
      "finagleService" -> v(templates("finagleService")),
      "ostrichServer" -> v(templates("ostrichService")),
      "finagleClients" -> v(
        if (s.options contains WithFinagleClient) Seq(finagleClient(service)) else Seq()
      ),
      "finagleServices" -> v(
        if (s.options contains WithFinagleService) Seq(finagleService(service)) else Seq()
      ),
      "ostrichServers" -> v(
        if (s.options contains WithOstrichServer) Seq(ostrichService(service)) else Seq()
      )
    )
  }
}
