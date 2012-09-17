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

trait ServiceTemplate extends Generator {
  self: JavaLike =>

  import Dictionary._

  def toDictionary(function: Function, async: Boolean): Dictionary = {
    val hasThrows = !async && function.throws.size > 0
    val throwsDictionaries =
      if (hasThrows) {
        function.throws map {
          t =>
            Dictionary("typeName" -> v(typeName(t.`type`)))
        }
      } else {
        Nil
      }
    Dictionary(
      "async" -> v(async),
      "docstring" -> v(function.docstring.getOrElse("")),
      "hasThrows" -> v(hasThrows),
      "throws" -> v(throwsDictionaries),
      "name" -> v(function.localName),
      "typeName" -> v(typeName(function.`type`)),
      "fieldParams" -> v(fieldParams(function.args))
    )
  }

  def serviceFunctionArgsStruct(f: Function): FunctionArgs = {
    FunctionArgs(f.localName + "_args", f.args)
  }

  def serviceFunctionResultStruct(f: Function): FunctionResult = {
    val throws = f.throws map {
      _.copy(requiredness = Requiredness.Optional)
    }
    val success = f.`type` match {
      case Void => Nil
      case fieldType: FieldType =>
        Seq(Field(0, "success", fieldType, None, Requiredness.Optional))
    }
    FunctionResult(f.localName + "_result", success ++ throws)
  }

  def finagleClient(s: Service) = {
    val parentName = s.parent.flatMap(_.service).map(_.name)
    Dictionary(
      "hasParent" -> v(parentName.nonEmpty),
      "parent" -> v(parentName map {
        _ + ".FinagledClient"
      } getOrElse ""),
      "functions" -> v(s.functions.map {
        f =>
          Dictionary(
            "header" -> templates("function"),
            "headerInfo" -> v(toDictionary(f, true)),
            "name" -> v(f.name),
            "type" -> v(typeName(f.`type`)),
            "void" -> v(f.`type` eq Void),
            "localName" -> v(f.localName),
            "argNames" -> v(f.args.map(_.name).mkString(", ")),
            "hasThrows" -> v(f.throws.size > 0),
            "throws" -> v(f.throws.map {
              thro => Dictionary("name" -> v(thro.name))
            })
          )
      }),
      "function" -> templates("finagleClientFunction")
    )
  }

  def finagleService(s: Service) = {
    val parentName = s.parent.flatMap(_.service).map(_.name)
    Dictionary(
      "hasParent" -> v(parentName.nonEmpty),
      "parent" -> v(parentName.map {
        _ + ".FinagledService"
      }.getOrElse(baseFinagleService)),
      "function" -> templates("finagleServiceFunction"),
      "functions" -> v(s.functions map {
        f =>
          Dictionary(
            "name" -> v(f.name),
            "localName" -> v(f.localName),
            "argNames" ->
              v(f.args map {
                "args." + _.name
              } mkString (", ")),
            "typeName" -> v(typeName(f.`type`)),
            "isVoid" -> v(f.`type` eq Void),
            "resultNamedArg" -> v(if (f.`type` ne Void) "success = Some(value)" else ""),
            "exceptions" -> v(f.throws map {
              t =>
                Dictionary(
                  "exceptionType" -> v(typeName(t.`type`)),
                  "fieldName" -> v(t.name)
                )
            })
          )
      })
    )
  }

  def ostrichService(s: Service) = Dictionary()

  def serviceDict(
    s: JavaService,
    namespace: String,
    includes: Seq[Include],
    serviceOptions: Set[ServiceOption]
  ) = {
    val service = s.service

    val parentName = service.parent.flatMap(_.service).map(_.name)
    Dictionary(
      "function" -> v(templates("function")),
      "package" -> v(namespace),
      "name" -> v(service.name),
      "docstring" -> v(service.docstring.getOrElse("")),
      "syncExtends" -> v(parentName.map {
        "extends " + _ + ".Iface "
      }.getOrElse("")),
      "asyncExtends" -> v(parentName.map {
        "extends " + _ + ".FutureIface "
      }.getOrElse("")),
      "syncFunctions" -> service.functions.map {
        f => toDictionary(f, false)
      },
      "asyncFunctions" -> service.functions.map {
        f => toDictionary(f, true)
      },
      "struct" -> v(templates("struct")),
      "structs" -> v(
        service.functions flatMap {
          f =>
            Seq(serviceFunctionArgsStruct(f), serviceFunctionResultStruct(f))
        } map {
          struct =>
            structDict(struct, None, includes, serviceOptions)
        }),
      "finagleClient" -> templates("finagleClient"),
      "finagleService" -> templates("finagleService"),
      "ostrichServer" -> templates("ostrichService"),
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
