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
import com.twitter.scrooge.frontend.ResolvedDocument
import com.twitter.scrooge.mustache.Dictionary
import com.twitter.scrooge.mustache.Dictionary._

trait ServiceTemplate { self: TemplateGenerator =>
  def functionDictionary(function: Function, generic: Option[String], namespace: Option[Identifier] = None): Dictionary = {
    val hasThrows = function.throws.size > 0
    val throwsDictionaries =
      if (hasThrows) {
        function.throws map { ex =>
          Dictionary(
            "throwType" -> genType(ex.fieldType),
            "throwName" -> genID(ex.sid))
        }
      } else {
        Nil
      }

    Dictionary(
      "generic" -> v(generic.map(v)),
      "docstring" -> v(function.docstring.getOrElse("")),
      "hasThrows" -> v(hasThrows),
      "throws" -> v(throwsDictionaries),
      "funcName" -> genID(function.funcName.toCamelCase),
      "funcObjectName" -> genID(functionObjectName(function)),
      "typeName" -> genType(function.funcType, namespace),
      "fieldParams" -> genFieldParams(function.args), // A list of parameters with types: (a: A, b: B...)
      "argNames" -> {
        val code = function.args.map { field => genID(field.sid).toData }.mkString(", ")
        v(code)
      },
      "argTypes" -> {
        function.args match {
          case Nil => v("Unit")
          case singleArg :: Nil => genType(singleArg.fieldType)
          case args =>
            val typesString = args.map { arg => genType(arg.fieldType) }.mkString(", ")
            v(s"($typesString)")
        }
      },
      "args" -> v(function.args.map { arg =>
        Dictionary("arg" -> genID(arg.sid))
      }),
      "isVoid" -> v(function.funcType == Void || function.funcType == OnewayVoid),
      "is_oneway" -> v(function.funcType == OnewayVoid)
    )
  }

  def functionArgsStruct(f:Function): FunctionArgs = {
    FunctionArgs(SimpleID("Args"),
      internalArgsStructNameForWire(f),
      f.args)
  }

  /**
   * Thrift Result struct that includes success or exceptions returned.
   */
  def resultStruct(f:Function): FunctionResult = {
    val throws = f.throws map {
      _.copy(requiredness = Requiredness.Optional)
    }
    val success = f.funcType match {
      case Void => None
      case OnewayVoid => None
      case fieldType: FieldType =>
        Some(Field(0, SimpleID("success"), "success", fieldType, None, Requiredness.Optional))
    }
    FunctionResult(
      SimpleID("Result"),
      resultStructNameForWire(f),
      success, throws
    )
  }

  def functionObjectName(f: Function): SimpleID = f.funcName.toTitleCase

  /**
   * The name used in RPC request, this needs to be same as Apache compiler
   */
  def internalArgsStructNameForWire(f: Function): String =
    f.funcName.name + "_args"

  /**
   * The name used in RPC request, this needs to be same as Apache compiler
   */
  private def resultStructNameForWire(f: Function): String =
    f.funcName.name + "_result"

  def finagleClient(
    service: Service,
    namespace: Identifier
  ) =
    Dictionary(
      "package" -> genID(namespace),
      "ServiceName" -> genID(service.sid.toTitleCase),
      "docstring" -> v(service.docstring.getOrElse("")),
      "hasParent" -> v(service.parent.isDefined),
      "parent" -> v(service.parent.map { p =>
        genID(getServiceParentID(p))
      }),
      "finagleClientParent" ->
        service.parent.map(getParentFinagleClient).getOrElse(v("")),
      "functions" -> v(service.functions.map {
        f =>
          Dictionary(
            "function" -> v(templates("function")),
            "functionInfo" -> v(functionDictionary(f, Some("Future"), Some(namespace))),
            "clientFuncNameForWire" -> v(f.originalName),
            "__stats_name" -> genID(f.funcName.toCamelCase.prepend("__stats_")),
            "type" -> genType(f.funcType),
            "isVoid" -> v(f.funcType == Void || f.funcType == OnewayVoid),
            "argNames" -> {
              val code = f.args.map { field => genID(field.sid).toData }.mkString(", ")
              v(code)
            }
          )
      }),
      "finagleClientFunction" -> v(templates("finagleClientFunction"))
    )

  def finagleService(
    service: Service,
    namespace: Identifier
  ) =
    Dictionary(
      "package" -> genID(namespace),
      "ServiceName" -> genID(service.sid.toTitleCase),
      "docstring" -> v(service.docstring.getOrElse("")),
      "hasParent" -> v(service.parent.isDefined),
      "finagleServiceParent" ->
        service.parent.map(getParentFinagleService).getOrElse(genBaseFinagleService),
      "function" -> v(templates("finagleServiceFunction")),
      "functions" -> v(service.functions map {
        f =>
          Dictionary(
            "serviceFuncNameForCompile" -> genID(f.funcName.toCamelCase),
            "serviceFuncNameForWire" -> v(f.originalName),
            "funcObjectName" -> genID(functionObjectName(f)),
            "argNames" ->
              v(f.args map { field =>
                "args." + genID(field.sid).toData
              } mkString (", ")),
            "typeName" -> genType(f.funcType),
            "isVoid" -> v(f.funcType == Void || f.funcType == OnewayVoid),
            "resultNamedArg" ->
              v(if (f.funcType != Void && f.funcType != OnewayVoid) "success = Some(value)" else ""),
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

  def unwrapArgs(arity: Int): String =
    arity match {
      case 0 => ""
      case 1 => "args"
      case _ =>
        (1 to arity).map { i =>
          s"args._$i"
        }.mkString(", ")
    }

  private[this] def getParentFunctions(service: Service): Option[(ServiceParent, Seq[Function])] =
    for {
      sp <- service.parent
      parentService <- sp.service
    } yield (sp, parentService.functions)

  /**
   * Collect and resolve inherited functions from services extended by the given service.
   * Does not include this service's functions.
   * @return a map from a FQN of the parent service to its functions.
   */
  private[this] def collectParentFunctions(service: Service): Map[Identifier, Seq[Function]] = {
    val builder = Map.newBuilder[Identifier, Seq[Function]]
    var next: Option[(ServiceParent, Seq[Function])] = getParentFunctions(service)
    var currentDoc: Option[ResolvedDocument] = None
    while (next.isDefined) {
      val (parent, functions) = next.get
      parent.doc match {
        case Some(doc) =>
          // Service B extends A; A is defined in another document (thrift file).
          builder += (parent.sid.addScope(getNamespace(doc.document)) -> functions)
          currentDoc = parent.doc
        case None if currentDoc.isDefined =>
          // C extends B extends A. A and B are in the same file, C in
          // another. A and B are in the same namepsace, so B.parent.doc is None.
          // We keep track of the current doc/namespace in currentDoc.
          builder += (parent.sid.addScope(getNamespace(currentDoc.get.document)) -> functions)
        case None =>
          // Parent is in the same file.
          builder += (getServiceParentID(parent) -> functions)
      }
      next = parent.service.flatMap { service =>
        getParentFunctions(service)
      }
    }
    builder.result()
  }

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
      "docstring" -> v(service.docstring.getOrElse("")),
      "syncParent" -> v(service.parent.map { p =>
        genID(getServiceParentID(p)).append(".Iface")
      }),
      "parent" -> v(service.parent.map { p =>
        genQualifiedID(getServiceParentID(p), namespace)
      }),
      "futureIfaceParent" -> v(service.parent.map { p =>
        genQualifiedID(getServiceParentID(p), namespace).append(".FutureIface")
      }),
      "genericParent" -> service.parent.map { p =>
        genID(getServiceParentID(p)).append("[MM]")
      }.getOrElse(v("ThriftService")),
      "syncFunctions" -> v(service.functions.map {
        f => functionDictionary(f, None)
      }),
      "asyncFunctions" -> v(service.functions.map {
        f => functionDictionary(f, Some("Future"))
      }),
      "genericFunctions" -> v(service.functions.map {
        f => functionDictionary(f, Some("MM"))
      }),
      "struct" -> v(templates("struct")),
      "thriftFunctions" -> v(service.functions.map { f =>
        Dictionary(
          "functionArgsStruct" ->
            v(structDict(
              functionArgsStruct(f),
              Some(namespace),
              includes,
              options)),
          "internalResultStruct" -> {
            val functionResult = resultStruct(f)
            v(structDict(
              functionResult,
              Some(namespace),
              includes,
              options) +
              Dictionary(
                "successFieldType" -> getSuccessType(functionResult, Some(namespace)),
                "successFieldValue" -> getSuccessValue(functionResult),
                "exceptionValues" -> getExceptionFields(functionResult)
              )
            )
          },
          "funcObjectName" -> genID(functionObjectName(f)),
          "unwrapArgs" -> v(unwrapArgs(f.args.length))
        ) + functionDictionary(f, Some("Future"), Some(namespace))
      }),
      "finagleClients" -> v(
        if (withFinagle) Seq(finagleClient(service, namespace)) else Seq()
      ),
      "finagleServices" -> v(
        if (withFinagle) Seq(finagleService(service, namespace)) else Seq()
      ),
      "withFinagle" -> v(withFinagle),
      "inheritedFunctions" -> {
        val ownFunctions: Seq[Dictionary] = service.functions.map {
          f => functionDictionary(f, Some("Future")) ++=
          (("ParentServiceName", v("self")))
        }
        val inheritedFunctions: Seq[Dictionary] =
          collectParentFunctions(service).flatMap {
            case (parentId: Identifier, functions: Seq[Function]) =>
              functions.map { f =>
                functionDictionary(f, Some("Future")) ++=
                (("ParentServiceName", genQualifiedID(parentId, namespace)))
              }
          }.toSeq
        v(ownFunctions ++ inheritedFunctions)
      }
    )
  }
}
