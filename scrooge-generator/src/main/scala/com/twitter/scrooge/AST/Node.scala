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
package com.twitter.scrooge.ast

sealed abstract class Node
abstract class ValueNode extends Node
abstract class TypeNode extends Node
abstract class DocumentNode extends Node
abstract class HeaderNode extends Node
abstract class DefinitionNode extends Node
abstract class IdNode extends Node

sealed abstract class Requiredness extends Node {
  /**
   * Indicates that the field is marked as optional in the IDL
   * and does not have a default value defined.
   */
  def isOptional: Boolean = this eq Requiredness.Optional

  /**
   * Indicates that the field is marked as required in the IDL.
   */
  def isRequired: Boolean = this eq Requiredness.Required

  /**
   * Indicates that the field is marked with neither optional
   * or required in the IDL (or optional with a default value).
   */
  def isDefault: Boolean = this eq Requiredness.Default
}

object Requiredness {
  /** @see [[Requiredness.isOptional]] */
  case object Optional extends Requiredness

  /** @see [[Requiredness.isRequired]] */
  case object Required extends Requiredness

  /** @see [[Requiredness.isDefault]] */
  case object Default extends Requiredness
}

case class Field(
    index: Int,
    sid: SimpleID,
    originalName: String,
    fieldType: FieldType,
    default: Option[RHS] = None,
    requiredness: Requiredness = Requiredness.Default,
    typeAnnotations: Map[String, String] = Map.empty,
    fieldAnnotations: Map[String, String] = Map.empty,
    docstring: Option[String] = None)
  extends Node

case class Function(
    funcName: SimpleID,
    originalName: String,
    funcType: FunctionType,
    args: Seq[Field],
    throws: Seq[Field],
    docstring: Option[String])
  extends Node

