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

  def internalArgsStruct(f:Function): FunctionArgs = {
    FunctionArgs(internalArgsStructName(f),
      internalArgsStructNameForWire(f),
      f.args)
  }

  def internalResultStruct(f:Function): FunctionResult = {
    val throws = f.throws map {
      _.copy(requiredness = Requiredness.Optional)
    }
    val success = f.funcType match {
      case Void => Nil
      case OnewayVoid => Nil
      case fieldType: FieldType =>
        Seq(Field(0, SimpleID("success"), "success", fieldType, None, Requiredness.Optional))
    }
    FunctionResult(internalResultStructName(f),
      internalResultStructNameForWire(f),
      success ++ throws)
  }

  def internalArgsStructName(f: Function): SimpleID =
    f.funcName.toCamelCase.append("$").append("args")

  /**
   * The name used in RPC request, this needs to be same as Apache compiler
   */
  def internalArgsStructNameForWire(f: Function): String =
    f.funcName.name + "_args"

  def internalResultStructName(f: Function): SimpleID =
    f.funcName.toCamelCase.append("$").append("result")

  /**
   * The name used in RPC request, this needs to be same as Apache compiler
   */
  def internalResultStructNameForWire(f: Function): String =
    f.funcName.name + "_result"

  def finagleClient(s: Service) = {
    Dictionary(
      "hasParent" -> v(s.parent.isDefined),
      "finagleClientParent" -> s.parent.map { p =>
        genID(SimpleID("FinagledClient").addScope(getServiceParentID(p)))
      }.getOrElse {codify("")},
      "functions" -> v(s.functions.map {
        f =>
          Dictionary(
            "header" -> v(templates("function")),
            "headerInfo" -> v(toDictionary(f, true)),
            "clientFuncNameForWire" -> codify(f.originalName),
            "__stats_name" -> genID(f.funcName.toCamelCase.prepend("__stats_")),
            "type" -> genType(f.funcType),
            "void" -> v(f.funcType eq Void),
            "ArgsStruct" -> genID(internalArgsStructName(f)),
            "ResultStruct" -> genID(internalResultStructName(f)),
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
    Dictionary(
      "hasParent" -> v(s.parent.isDefined),
      "finagleServiceParent" -> {s.parent match {
        case Some(p) =>
          genID(SimpleID("FinagledService").addScope(getServiceParentID(p)))
        case None => genBaseFinagleService
      }},
      "function" -> v(templates("finagleServiceFunction")),
      "functions" -> v(s.functions map {
        f =>
          Dictionary(
            "serviceFuncNameForCompile" -> genID(f.funcName.toCamelCase),
            "serviceFuncNameForWire" -> codify(f.originalName),
            "ArgsStruct" -> genID(internalArgsStructName(f)),
            "ResultStruct" -> genID(internalResultStructName(f)),
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

    Dictionary(
      "function" -> v(templates("function")),
      "package" -> genID(namespace),
      "ServiceName" -> genID(service.sid.toTitleCase),
      "docstring" -> codify(service.docstring.getOrElse("")),
      "syncExtends" -> codify(service.parent.map { p =>
          "extends " + genID(getServiceParentID(p)) + ".Iface "
        }.getOrElse("")),
      "asyncExtends" -> codify(service.parent.map { p =>
        "extends " + genID(getServiceParentID(p)) + ".FutureIface "
      }.getOrElse("")),
      "syncFunctions" -> v(service.functions.map {
        f => toDictionary(f, false)
      }),
      "asyncFunctions" -> v(service.functions.map {
        f => toDictionary(f, true)
      }),
      "struct" -> v(templates("struct")),
      "internalStructs" -> v(service.functions.map { f =>
        Dictionary(
          "internalArgsStruct" -> v(structDict(
            internalArgsStruct(f),
            None, includes, serviceOptions)),
          "internalResultStruct" -> v(structDict(
            internalResultStruct(f),
            None, includes, serviceOptions))
        )
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
      ),
      "withFinagleClient" -> v(s.options contains WithFinagleService),
      "withFinagleService" -> v(s.options contains WithFinagleService),
      "withOstrichServer" -> v(s.options contains WithOstrichServer),
      "withFinagle" -> v((s.options contains WithFinagleClient)
        || (s.options contains WithFinagleService)),
      "date" -> codify(generationDate),
      "enablePassthrough" -> v(enablePassthrough)
    )
  }
}
