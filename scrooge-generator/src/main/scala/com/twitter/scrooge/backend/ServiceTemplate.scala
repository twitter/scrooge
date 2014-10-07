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

trait ServiceTemplate { self: TemplateGenerator =>
  def toDictionary(function: Function, generic: Option[String]): Dictionary = {
    val hasThrows = generic.isEmpty && function.throws.size > 0
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
      "generic" -> v(generic.map(codify)),
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

  def finagleClient(
    service: Service,
    namespace: Identifier
  ) =
    Dictionary(
      "package" -> genID(namespace),
      "ServiceName" -> genID(service.sid.toTitleCase),
      "docstring" -> codify(service.docstring.getOrElse("")),
      "hasParent" -> v(service.parent.isDefined),
      "finagleClientParent" ->
        service.parent.map(getParentFinagleClient).getOrElse(codify("")),
      "functions" -> v(service.functions.map {
        f =>
          Dictionary(
            "header" -> v(templates("function")),
            "headerInfo" -> v(toDictionary(f, Some("Future"))),
            "clientFuncNameForWire" -> codify(f.originalName),
            "__stats_name" -> genID(f.funcName.toCamelCase.prepend("__stats_")),
            "type" -> genType(f.funcType),
            "isVoid" -> v(f.funcType == Void || f.funcType == OnewayVoid),
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

  def finagleService(
    service: Service,
    namespace: Identifier
  ) =
    Dictionary(
      "package" -> genID(namespace),
      "ServiceName" -> genID(service.sid.toTitleCase),
      "docstring" -> codify(service.docstring.getOrElse("")),
      "hasParent" -> v(service.parent.isDefined),
      "finagleServiceParent" ->
        service.parent.map(getParentFinagleService).getOrElse(genBaseFinagleService),
      "function" -> v(templates("finagleServiceFunction")),
      "functions" -> v(service.functions map {
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
            "isVoid" -> v(f.funcType == Void || f.funcType == OnewayVoid),
            "resultNamedArg" ->
              codify(if (f.funcType != Void && f.funcType != OnewayVoid) "success = Some(value)" else ""),
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

  def serviceDict(
    service: Service,
    namespace: Identifier,
    includes: Seq[Include],
    options: Set[ServiceOption]
  ) = {
    val withFinagle = options.contains(WithFinagle)
    Dictionary(
      "function" -> v(templates("function")),
      "package" -> genID(namespace),
      "ServiceName" -> genID(service.sid.toTitleCase),
      "docstring" -> codify(service.docstring.getOrElse("")),
      "syncParent" -> v(service.parent.map { p =>
        genID(getServiceParentID(p)).append(".Iface")
      }),
      "futureIfaceParent" -> v(service.parent.map { p =>
        genID(getServiceParentID(p)).append(".FutureIface")
      }),
      "genericParent" -> service.parent.map { p =>
        genID(getServiceParentID(p)).append("[MM]")
      }.getOrElse(codify("ThriftService")),
      "syncFunctions" -> v(service.functions.map {
        f => toDictionary(f, None)
      }),
      "asyncFunctions" -> v(service.functions.map {
        f => toDictionary(f, Some("Future"))
      }),
      "genericFunctions" -> v(service.functions.map {
        f => toDictionary(f, Some("MM"))
      }),
      "struct" -> v(templates("struct")),
      "internalStructs" -> v(service.functions.map { f =>
        Dictionary(
          "internalArgsStruct" -> v(structDict(
            internalArgsStruct(f),
            None, includes, options)),
          "internalResultStruct" -> v(structDict(
            internalResultStruct(f),
            None, includes, options))
        )
      }),
      "finagleClients" -> v(
        if (withFinagle) Seq(finagleClient(service, namespace)) else Seq()
      ),
      "finagleServices" -> v(
        if (withFinagle) Seq(finagleService(service, namespace)) else Seq()
      ),
      "withFinagle" -> v(withFinagle)
    )
  }
}
