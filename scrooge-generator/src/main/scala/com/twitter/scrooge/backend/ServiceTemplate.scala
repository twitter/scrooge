package com.twitter.scrooge.backend

/**
 * Copyright 2011 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.twitter.scrooge.ast._
import com.twitter.scrooge.frontend.ResolvedService
import com.twitter.scrooge.mustache.Dictionary
import com.twitter.scrooge.mustache.Dictionary._
import scala.collection.mutable

trait ServiceTemplate { self: TemplateGenerator =>
  def functionDictionary(function: Function, generic: Option[String]): Dictionary = {
    val hasThrows = function.throws.nonEmpty
    val throwsDictionaries =
      if (hasThrows) {
        function.throws map { ex =>
          Dictionary("throwType" -> genType(ex.fieldType), "throwName" -> genID(ex.sid))
        }
      } else {
        Nil
      }

    val argNames = function.args.map { field => genID(field.sid).toData }

    Dictionary(
      "generic" -> v(generic.map(v)),
      "docstring" -> v(function.docstring.getOrElse("")),
      "hasThrows" -> v(hasThrows),
      "throws" -> v(throwsDictionaries),
      "funcName" -> genID(function.funcName.toCamelCase),
      "originalFuncName" -> v(function.originalName),
      "funcObjectName" -> genID(functionObjectName(function)),
      "typeName" -> genType(function.funcType),
      "fieldParams" -> genFieldParams(
        function.args
      ), // A list of parameters with types: (a: A, b: B...)
      "argNames" -> v(argNames.mkString(", ")),
      "argsFieldNames" -> {
        val code = argNames
          .map { field => s"args.$field" }
          .mkString(", ")
        v(code)
      },
      "argTypes" -> {
        function.args match {
          case Nil => v("Unit")
          case singleArg :: Nil => genType(singleArg.fieldType)
          case args =>
            val typesString = args
              .map { arg => genType(arg.fieldType) }
              .mkString(", ")
            v(s"($typesString)")
        }
      },
      "args" -> v(function.args.map { arg => Dictionary("arg" -> genID(arg.sid)) }),
      "isVoid" -> v(function.funcType == Void || function.funcType == OnewayVoid),
      "is_oneway" -> v(function.funcType == OnewayVoid),
      "functionType" -> {
        val returnType = s"Future[${genType(function.funcType)}]"
        val types = s"[Args,$returnType]"
        v(s"Function1$types")
      },
      "reqRepFunctionType" -> {
        val returnType =
          s"Future[_root_.com.twitter.scrooge.Response[${genType(function.funcType)}]]"
        val types = s"[_root_.com.twitter.scrooge.Request[Args],$returnType]"
        v(s"Function1$types")
      },
      "moreThan22Args" -> v(function.args.size > 22)
    )
  }

  def functionArgsStruct(f: Function): FunctionArgs = {
    FunctionArgs(SimpleID("Args"), internalArgsStructNameForWire(f), f.args)
  }

  /**
   * Thrift Result struct that includes success or exceptions returned.
   */
  def resultStruct(f: Function): FunctionResult = {
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
      success,
      throws
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

  def finagleClient(service: Service, namespace: Identifier, withAsClosable: Boolean): Dictionary =
    Dictionary(
      "package" -> genID(namespace),
      "ServiceName" -> genID(service.sid.toTitleCase),
      "docstring" -> v(service.docstring.getOrElse("")),
      "hasParent" -> v(service.parent.isDefined),
      "parent" -> v(service.parent.map { p => genID(getServiceParentID(p)) }),
      "finagleClientParent" ->
        service.parent.map(getParentFinagleClient).getOrElse(v("")),
      "functions" -> v(service.functions.map { f =>
        Dictionary(
          "function" -> v(templates("function")),
          "functionInfo" -> v(functionDictionary(f, Some("Future"))),
          "clientFuncNameForWire" -> v(f.originalName),
          "__stats_name" -> genID(f.funcName.toCamelCase.prepend("__stats_")),
          "type" -> genType(f.funcType),
          "isVoid" -> v(f.funcType == Void || f.funcType == OnewayVoid),
          "argNames" -> {
            val code = f.args
              .map { field => genID(field.sid).toData }
              .mkString(", ")
            v(code)
          }
        )
      }),
      "hasFunctions" -> v(service.functions.nonEmpty),
      "finagleClientFunction" -> v(templates("finagleClientFunction")),
      "withAsClosable" -> v(withAsClosable)
    )

  def finagleService(service: Service, namespace: Identifier): Dictionary =
    Dictionary(
      "package" -> genID(namespace),
      "ServiceName" -> genID(service.sid.toTitleCase),
      "docstring" -> v(service.docstring.getOrElse("")),
      "hasParent" -> v(service.parent.isDefined),
      "finagleServiceParent" ->
        service.parent.map(getParentFinagleService).getOrElse(genBaseFinagleService),
      "methodService" -> v(templates("methodService")),
      "methodServices" -> v(service.functions map { f =>
        Dictionary(
          "methodSvcNameForCompile" -> genID(f.funcName.toCamelCase),
          "methodSvcNameForWire" -> v(f.originalName),
          "__stats_name" -> genID(f.funcName.toCamelCase.prepend("__stats_")),
          "funcObjectName" -> genID(functionObjectName(f)),
          "argNames" ->
            v(
              f.args
                .map { field => "args." + genID(field.sid).toData }
                .mkString(", ")
            ),
          "typeName" -> genType(f.funcType),
          "isVoid" -> v(f.funcType == Void || f.funcType == OnewayVoid),
          "resultNamedArg" ->
            v(if (f.funcType != Void && f.funcType != OnewayVoid) "success = Some(resTry.apply())"
            else ""),
          "exceptions" -> v(f.throws.zipWithIndex map {
            case (t, index) =>
              Dictionary(
                "exceptionType" -> genType(t.fieldType),
                "fieldName" -> genID(t.sid),
                "first" -> v(index == 0),
                "last" -> v(index == f.throws.size - 1)
              )
          }),
          "hasExceptions" -> v(f.throws.nonEmpty)
        )
      }),
      "hasMethodServices" -> v(service.functions.nonEmpty)
    )

  def unwrapArgs(arity: Int): String =
    arity match {
      case 0 => ""
      case 1 => "args"
      case _ =>
        (1 to arity)
          .map { i => s"args._$i" }
          .mkString(", ")
    }

  private def basename(fqdn: String): String = fqdn.split('.').last

  def serviceDict(
    service: Service,
    namespace: Identifier,
    includes: Seq[Include],
    options: Set[ServiceOption],
    genAdapt: Boolean
  ): Dictionary = {
    val withFinagle = options.contains(WithFinagle)
    val withAsClosable = options.contains(WithAsClosable)
    lazy val computeInheritedFunctions: Service => (Seq[Dictionary], Seq[Dictionary]) = { service =>
      {
        // For service-per-endpoint, we generate a class with a value for each method, so
        // method names must be unique.
        val deduper = new NameDeduplicator()
        val inheritedFunctions: Seq[Dictionary] =
          // Note: inherited functions must be deduped first, so we walk the parent chain
          // from the topmost parent down (hence the reverse).
          resolvedDoc
            .resolveParentServices(service, namespaceLanguage, defaultNamespace)
            .reverse
            .flatMap { result: ResolvedService =>
              result.service.functions.map { function =>
                Dictionary(
                  "ParentServiceName" -> genID(result.serviceID),
                  "funcName" -> genID(function.funcName.toCamelCase),
                  "dedupedFuncName" -> genID(deduper.dedupe(function.funcName.toCamelCase)),
                  "funcObjectName" -> genID(functionObjectName(function)),
                  "withFuncName" -> genID(deduper.dedupe(function.funcName.toTitleCase))
                )
              }
            }
        val ownFunctions: Seq[Dictionary] = service.functions.map { function =>
          functionDictionary(function, Some("Future")) ++= (
            "ParentServiceName" -> v("self"),
            "dedupedFuncName" -> genID(deduper.dedupe(function.funcName.toCamelCase)),
            "withFuncName" -> genID(deduper.dedupe(function.funcName.toTitleCase))
          )
        }
        (inheritedFunctions, ownFunctions)
      }
    }

    val pkg = genID(namespace)
    val pkgName = v(basename(pkg.toData))

    Dictionary(
      "function" -> v(templates("function")),
      "package" -> pkg,
      "packageName" -> pkgName,
      "ServiceName" -> genID(service.sid.toTitleCase),
      "docstring" -> v(service.docstring.getOrElse("")),
      "syncParent" -> v(service.parent.map { p => genID(getServiceParentID(p)).append(".Iface") }),
      "parent" -> v(service.parent.map { p => genQualifiedID(getServiceParentID(p), namespace) }),
      "methodPerEndpointParent" -> v(service.parent.map { p =>
        genQualifiedID(getServiceParentID(p), namespace).append(".MethodPerEndpoint")
      }),
      "futureIfaceParent" -> v(service.parent.map { p =>
        genQualifiedID(getServiceParentID(p), namespace).append(".FutureIface")
      }),
      "genericParent" -> service.parent
        .map { p => genID(getServiceParentID(p)).append("[MM]") }
        .getOrElse(v("_root_.com.twitter.finagle.thrift.ThriftService")),
      "syncFunctions" -> v(service.functions.map { f => functionDictionary(f, None) }),
      "asyncFunctions" -> v(service.functions.map { f => functionDictionary(f, Some("Future")) }),
      "genericFunctions" -> v(service.functions.map { f => functionDictionary(f, Some("MM")) }),
      "struct" -> v(templates("struct")),
      "thriftFunctions" -> v(service.functions.map { f =>
        Dictionary(
          "functionArgsStruct" ->
            v(structDict(functionArgsStruct(f), Some(namespace), includes, options, genAdapt)),
          "internalResultStruct" -> {
            val functionResult = resultStruct(f)
            v(
              structDict(functionResult, Some(namespace), includes, options, genAdapt) +
                Dictionary(
                  "successFieldType" -> getSuccessType(functionResult),
                  "successFieldValue" -> getSuccessValue(functionResult),
                  "exceptionValues" -> getExceptionFields(functionResult)
                )
            )
          },
          "annotations" -> TemplateGenerator.renderPairs(f.annotations),
          "funcObjectName" -> genID(functionObjectName(f)),
          "unwrapArgs" -> v(unwrapArgs(f.args.length))
        ) + functionDictionary(f, Some("Future"))
      }),
      "hasThriftFunctions" -> v(service.functions.nonEmpty),
      "finagleClients" -> v(
        if (withFinagle) Seq(finagleClient(service, namespace, withAsClosable)) else Seq()
      ),
      "finagleServices" -> v(
        if (withFinagle) Seq(finagleService(service, namespace)) else Seq()
      ),
      // scalac 2.11 fails to compile classes with more than 254 method arguments
      // due to https://issues.scala-lang.org/browse/SI-7324
      // We skip generation of ServiceIfaces for thrift services with 255+ methods.
      "generateServiceIface" -> {
        val numParentFunctions = resolvedDoc
          .collectParentServices(service)
          .map {
            case (_, service) => service.functions.length
          }
          .sum
        val totalFunctions = service.functions.length + numParentFunctions
        v(totalFunctions <= 254)
      },
      "withFinagle" -> v(withFinagle),
      "inheritedFunctions" -> {
        val (inheritedParentFunctions, ownFunctions) = computeInheritedFunctions(service)
        v(ownFunctions ++ inheritedParentFunctions)
      },
      "inheritedParentFunctions" -> {
        val (inheritedParentFunctions, _) = computeInheritedFunctions(service)
        v(inheritedParentFunctions)
      },
      "ownFunctions" -> {
        val (_, ownFunctions) = computeInheritedFunctions(service)
        v(ownFunctions)
      },
      "annotations" -> TemplateGenerator.renderPairs(service.annotations),
      "withAsClosable" -> v(withAsClosable),
      "methodFilter" -> v(templates("methodFilter")),
      "methodFilters" -> v(service.functions map { f =>
        Dictionary(
          "methodSvcNameForCompile" -> genID(f.funcName.toCamelCase),
          "methodSvcNameForWire" -> v(f.originalName),
          "funcObjectName" -> genID(functionObjectName(f)),
          "argNames" ->
            v(
              f.args
                .map { field => "args." + genID(field.sid).toData }
                .mkString(", ")
            ),
          "typeName" -> genType(f.funcType),
          "resultNamedArg" ->
            v(if (f.funcType != Void && f.funcType != OnewayVoid) "success = Some(resTry.apply())"
            else ""),
          "exceptions" -> v(f.throws.zipWithIndex map {
            case (t, index) =>
              Dictionary(
                "exceptionType" -> genType(t.fieldType),
                "fieldName" -> genID(t.sid),
                "first" -> v(index == 0),
                "last" -> v(index == f.throws.size - 1)
              )
          }),
          "hasExceptions" -> v(f.throws.nonEmpty)
        )
      })
    )
  }

  private[this] class NameDeduplicator() {
    private[this] val seenIDs = new mutable.HashSet[String]

    /**
     * Append a '_' to deduplicate function names for the case class members.
     * This also stores the new ID in the set of seen IDs.
     */
    def dedupe(id: SimpleID): SimpleID = {
      var currentID = id
      while (seenIDs.contains(currentID.toString)) {
        currentID = currentID.append("_")
      }
      seenIDs.add(currentID.toString)
      currentID
    }
  }
}
