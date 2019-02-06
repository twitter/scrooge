/*
 * Copyright 2014 Twitter, Inc.
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

package com.twitter.scrooge.linter

import com.twitter.scrooge.ast._
import org.scalatest.{MustMatchers, WordSpec}

class LinterSpec extends WordSpec with MustMatchers {

  def mustPass(errors: Iterable[LintMessage]) =
    errors.size must equal(0)

  def genFields(n: Int = 1): Seq[Field] = (0 until n) map { i =>
    Field(
      i,
      SimpleID(s"val$i"),
      s"val$i",
      TString,
      default = Some(StringLiteral(s"val-$i")),
      requiredness = Requiredness.Required
    )
  }

  "Linter" should {
    "pass Namespaces" in {
      mustPass(
        LintRule.Namespaces(
          Document(
            Seq(
              Namespace("java", Identifier("com.twitter.oatmeal")),
              Namespace("scala", Identifier("com.twitter.oatmeal"))
            ),
            Nil
          )
        )
      )
    }

    "fail Namespaces" in {
      val errors =
        LintRule.Namespaces(Document(Seq(Namespace("java", SimpleID("asdf"))), Nil)).toSeq
      errors.length must be(1)
      assert(errors(0).msg contains ("Missing namespace"))
    }

    "pass RelativeIncludes" in {
      mustPass(
        LintRule.RelativeIncludes(
          Document(
            Seq(
              Namespace("java", SimpleID("asdf")),
              Include("com.twitter.oatmeal", Document(Seq(), Seq()))
            ),
            Nil
          )
        )
      )
    }

    "fail RelativeIncludes" in {
      val errors = LintRule
        .RelativeIncludes(
          Document(
            Seq(
              Namespace("java", SimpleID("asdf")),
              Include("./dir1/../dir1/include1.thrift", Document(Seq(), Seq()))
            ),
            Nil
          )
        )
        .toSeq
      errors.size must be(1)
      assert(errors(0).msg contains ("Relative include path found"))
    }

    "pass CamelCase" in {
      mustPass(
        LintRule.CamelCase(
          Document(
            Seq(),
            Seq(
              Struct(
                SimpleID("SomeType"),
                "SomeType",
                Seq(Field(1, SimpleID("camelCaseFieldName"), "camelCaseFieldName", TString)),
                None
              )
            )
          )
        )
      )
    }

    "fail CamelCase" in {
      val errors = LintRule
        .CamelCase(
          Document(
            Seq(),
            Seq(
              Struct(
                SimpleID("SomeType"),
                "SomeType",
                Seq(Field(1, SimpleID("non_camel_case"), "non_camel_case", TString)),
                None
              )
            )
          )
        )
        .toSeq
      errors.length must be(1)
      assert(errors(0).msg contains ("lowerCamelCase"))
    }

    def struct(name: String, fields: Map[String, FieldType], persisted: Boolean = false) =
      Struct(
        SimpleID(name),
        name,
        fields.zipWithIndex.map {
          case ((fieldName, fieldType), i) => Field(i, SimpleID(fieldName), fieldName, fieldType)
        }.toSeq,
        None,
        if (persisted) Map("persisted" -> "true") else Map.empty
      )

    "fail TransitivePersistence" in {
      val errors = LintRule
        .TransitivePersistence(
          Document(
            Seq(),
            Seq(
              struct(
                "SomeType",
                Map(
                  "foo" -> TString,
                  "bar" -> StructType(struct("SomeOtherType", Map.empty))
                ),
                true
              )
            )
          )
        )
        .toSeq
      errors.length must be(1)
      val error = errors(0).msg
      assert(error.contains("persisted"))
      assert(error.contains("SomeType"))
      assert(error.contains("SomeOtherType"))
    }

    "pass TransitivePersistence" in {
      mustPass(
        LintRule.TransitivePersistence(
          Document(
            Seq(),
            Seq(
              struct(
                "SomeType",
                Map(
                  "foo" -> TString,
                  "bar" -> StructType(struct("SomeOtherType", Map.empty, true))
                ),
                true
              )
            )
          )
        )
      )
    }

    "fail DocumentedPersisted" in {
      val errors = LintRule
        .DocumentedPersisted(
          Document(
            Seq(),
            Seq(
              struct(
                "SomeType",
                Map(
                  "foo" -> TString
                ),
                true
              )
            )
          )
        )
        .toSeq
      errors.length must be(2)
      assert(errors.forall(_.level == Warning))
      val structError = errors.head.msg
      assert(structError.contains("SomeType"))
      val fieldError = errors(1).msg
      assert(fieldError.contains("foo"))
      assert(fieldError.contains("SomeType"))
    }

    "pass DocumentedPersisted" in {
      mustPass(
        LintRule.DocumentedPersisted(
          Document(
            Seq(),
            Seq(
              Struct(
                SimpleID("SomeType"),
                "SomeType",
                Seq(Field(1, SimpleID("foo"), "foo", TString, docstring = Some("blah blah"))),
                docstring = Some("documented struct is documented"),
                Map("persisted" -> "true")
              )
            )
          )
        )
      )
    }

    "pass RequiredFieldDefault" in {
      mustPass(
        LintRule.RequiredFieldDefault(
          Document(
            Seq(),
            Seq(
              Struct(
                SimpleID("SomeType"),
                "SomeType",
                Seq(
                  Field(
                    1,
                    SimpleID("f1"),
                    "f1",
                    TString,
                    default = Some(StringLiteral("v1")),
                    requiredness = Requiredness.Optional
                  ),
                  Field(
                    2,
                    SimpleID("f2"),
                    "f2",
                    TString,
                    default = None,
                    requiredness = Requiredness.Required
                  )
                ),
                None
              )
            )
          )
        )
      )
    }

    "fail RequiredFieldDefault" in {
      val errors = LintRule
        .RequiredFieldDefault(
          Document(
            Seq(),
            Seq(
              Struct(
                SimpleID("SomeType"),
                "SomeType",
                Seq(
                  Field(
                    1,
                    SimpleID("f1"),
                    "f1",
                    TString,
                    default = Some(StringLiteral("v1")),
                    requiredness = Requiredness.Required
                  )
                ),
                None
              )
            )
          )
        )
        .toSeq
      errors.length must be(1)
      assert(errors(0).msg contains ("Required field"))
    }

    "pass Keywords" in {
      mustPass(
        LintRule.Keywords(
          Document(
            Seq(),
            Seq(
              Struct(
                SimpleID("SomeType"),
                "SomeType",
                Seq(
                  Field(
                    1,
                    SimpleID("klass"),
                    "klass",
                    TString,
                    default = Some(StringLiteral("v1")),
                    requiredness = Requiredness.Optional
                  ),
                  Field(
                    2,
                    SimpleID("notAKeyWord"),
                    "notAKeyWord",
                    TString,
                    default = None,
                    requiredness = Requiredness.Required
                  )
                ),
                None
              )
            )
          )
        )
      )
    }

    "fail Keywords" in {
      val errors = LintRule
        .Keywords(
          Document(
            Seq(),
            Seq(
              Struct(
                SimpleID("SomeType"),
                "SomeType",
                Seq(
                  Field(
                    1,
                    SimpleID("val"),
                    "val",
                    TString,
                    default = Some(StringLiteral("v1")),
                    requiredness = Requiredness.Optional
                  )
                ),
                None
              )
            )
          )
        )
        .toSeq
      errors.length must be(1)
      assert(errors(0).msg contains ("Avoid using keywords"))
    }

    "pass non negative index" in {
      mustPass(
        LintRule.FieldIndexGreaterThanZeroRule(
          Document(
            Seq(),
            Seq(
              Struct(
                SimpleID("SomeType"),
                "SomeType",
                Seq(
                  Field(
                    1,
                    SimpleID("val"),
                    "val",
                    TString,
                    default = Some(StringLiteral("v1")),
                    requiredness = Requiredness.Optional
                  )
                ),
                None
              )
            )
          )
        )
      )
    }

    "warn non negative index" in {
      val errors = LintRule
        .FieldIndexGreaterThanZeroRule(
          Document(
            Seq(),
            Seq(
              Struct(
                SimpleID("SomeType"),
                "SomeType",
                Seq(
                  Field(
                    -1,
                    SimpleID("val"),
                    "val",
                    TString,
                    default = Some(StringLiteral("v1")),
                    requiredness = Requiredness.Optional
                  )
                ),
                None
              )
            )
          )
        )
        .toSeq
      errors.length must be(1)
      assert(errors(0).msg contains ("Field id should be supplied"))
    }

    "warn on max struct fields" in {
      val warnings = LintRule
        .CompilerOptimizedMethodParamLimit(
          Document(Seq(), Seq(FunctionArgs(SimpleID("SomeType"), "SomeType", genFields(100))))
        )
        .toSeq
      warnings.size must be(1)
      assert(warnings(0).msg contains ("fields for thrift struct"))
      assert(warnings(0).msg contains ("SomeType"))
    }

    "warn on max service function fields" in {
      val warnings = LintRule
        .CompilerOptimizedMethodParamLimit(
          Document(
            Seq(),
            Seq(
              Service(
                SimpleID("SomeType"),
                parent = None,
                functions = Seq(
                  Function(
                    SimpleID("someFunc"),
                    "someFunc",
                    TString,
                    genFields(100),
                    throws = Seq(),
                    docstring = None
                  )
                ),
                Some("SomeType")
              )
            )
          )
        )
        .toSeq
      warnings.size must be(1)
      assert(warnings(0).msg contains ("thrift service method parameters"))
      assert(warnings(0).msg contains ("SomeType.someFunc"))
    }

    "warn on max service function exception fields" in {
      val warnings = LintRule
        .CompilerOptimizedMethodParamLimit(
          Document(
            Seq(),
            Seq(
              Service(
                SimpleID("SomeType"),
                parent = None,
                functions = Seq(
                  Function(
                    SimpleID("someFunc"),
                    "someFunc",
                    TString,
                    args = Seq(),
                    throws = genFields(100),
                    docstring = None
                  )
                ),
                Some("SomeType")
              )
            )
          )
        )
        .toSeq
      warnings.size must be(1)
      assert(warnings(0).msg contains ("thrift service method exceptions"))
      assert(warnings(0).msg contains ("SomeType.someFunc"))
    }

    "warn on max service functions " in {
      val funcs = (0 until 100) map { i =>
        Function(SimpleID(s"val$i"), s"val$i", TString, Seq(), Seq(), None)
      }

      val warnings = LintRule
        .CompilerOptimizedMethodParamLimit(
          Document(
            Seq(),
            Seq(Service(SimpleID("SomeType"), parent = None, functions = funcs, Some("SomeType")))
          )
        )
        .toSeq
      warnings.size must be(1)
      assert(warnings(0).msg contains ("thrift service methods"))
      assert(warnings(0).msg contains ("SomeType"))
    }

    "detect malformed comment string" in {
      val expectedMessage = "Malformed Docstring"

      val invalidCommentStrings = Seq("/** /* */")
      invalidCommentStrings.foreach { docString: String =>
        val funcField = LintRule
          .MalformedDocstring(
            Document(
              Seq(),
              Seq(
                FunctionArgs(
                  SimpleID("SomeType"),
                  "SomeType",
                  genFields().map(_.copy(docstring = Some(docString)))
                )
              )
            )
          )
          .toSeq
        funcField.size must be(1)
        assert(funcField(0).msg contains (expectedMessage))

        val structLike = LintRule
          .MalformedDocstring(
            Document(
              Seq(),
              Seq(Struct(SimpleID("SomeType"), "SomeType", genFields(), Some(docString)))
            )
          )
          .toSeq
        structLike.size must be(1)
        assert(structLike(0).msg contains (expectedMessage))

        val structLikeField = LintRule
          .MalformedDocstring(
            Document(
              Seq(),
              Seq(
                Struct(
                  SimpleID("SomeType"),
                  "SomeType",
                  Seq(
                    Field(
                      1,
                      SimpleID("someField"),
                      "someField",
                      TString,
                      docstring = Some(docString)
                    )
                  ),
                  None
                )
              )
            )
          )
          .toSeq
        structLikeField.size must be(1)
        assert(structLikeField(0).msg contains (expectedMessage))

        val const = LintRule
          .MalformedDocstring(
            Document(
              Seq(),
              Seq(
                ConstDefinition(
                  SimpleID("SomeType"),
                  TString,
                  StringLiteral("test"),
                  Some(docString)
                )
              )
            )
          )
          .toSeq
        const.size must be(1)
        assert(const(0).msg contains (expectedMessage))

        val enum = LintRule
          .MalformedDocstring(
            Document(
              Seq(),
              Seq(
                Enum(
                  SimpleID("SomeType"),
                  Seq(EnumField(SimpleID("enumField"), 0, None)),
                  Some(docString)
                )
              )
            )
          )
          .toSeq
        enum.size must be(1)
        assert(enum(0).msg contains (expectedMessage))

        val enumField = LintRule
          .MalformedDocstring(
            Document(
              Seq(),
              Seq(EnumField(SimpleID("enumField"), 0, Some(docString)))
            )
          )
          .toSeq
        enumField.size must be(1)
        assert(enumField(0).msg contains (expectedMessage))
      }

    }

    "pass basic key types for maps" in {
      mustPass(
        LintRule.MapKeyType(
          Document(
            Seq(),
            Seq(
              Struct(
                SimpleID("SomeType"),
                "SomeType",
                Seq(
                  Field(
                    1,
                    SimpleID("val"),
                    "val",
                    MapType(TString, TString, None)
                  )
                ),
                None
              ),
              ConstDefinition(
                SimpleID("SomeConst"),
                MapType(TString, TString, None),
                MapRHS(Seq()),
                None
              ),
              Typedef(
                SimpleID("SomeTypeDef"),
                MapType(TString, TString, None)
              )
            )
          )
        )
      )
    }

    "warn on maps with complex key types" in {
      val complexType = StructType(
        Struct(
          SimpleID("SomeStruct"),
          "SomeStruct",
          Seq(
            Field(
              1,
              SimpleID("val"),
              "val",
              TString
            )
          ),
          None
        )
      )

      val warnings = LintRule.MapKeyType(
        Document(
          Seq(),
          Seq(
            Struct(
              SimpleID("SomeType"),
              "SomeType",
              Seq(
                Field(
                  1,
                  SimpleID("val"),
                  "val",
                  MapType(complexType, TString, None)
                )
              ),
              None
            ),
            ConstDefinition(
              SimpleID("SomeConst"),
              MapType(complexType, TString, None),
              MapRHS(Seq()),
              None
            ),
            Typedef(
              SimpleID("SomeTypeDef"),
              MapType(complexType, TString, None)
            )
          )
        )
      )

      warnings.size must be(3)
    }
  }
}
