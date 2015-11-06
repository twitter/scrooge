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
import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.MustMatchers


@RunWith(classOf[JUnitRunner])
class LinterSpec extends WordSpec with MustMatchers {

  def mustPass(errors: Iterable[LintMessage]) =
    errors.size must equal(0)

  "Linter" should {
    "pass Namespaces" in {
      mustPass(
        LintRule.Namespaces(Document(Seq(
          Namespace("java", Identifier("com.twitter.oatmeal")),
          Namespace("scala", Identifier("com.twitter.oatmeal"))
        ), Nil))
      )
    }

    "fail Namespaces" in {
      val errors = LintRule.Namespaces(Document(Seq(Namespace("java", SimpleID("asdf"))), Nil)).toSeq
      errors.length must be(1)
      assert(errors.head.msg contains "Missing namespace")
    }

    "pass RelativeIncludes" in {
      mustPass(
        LintRule.RelativeIncludes(
          Document(
            Seq(
              Namespace("java", SimpleID("asdf")),
              Include("com.twitter.oatmeal", Document(Seq(), Seq()))),
            Nil))
      )
    }

    "fail RelativeIncludes" in {
      val errors = LintRule.RelativeIncludes(Document(
        Seq(
          Namespace("java", SimpleID("asdf")),
          Include("./dir1/../dir1/include1.thrift", Document(Seq(), Seq()))),
        Nil))
      errors.size must be(1)
      assert(errors.head.msg contains "Relative include path found")
    }

    "pass CamelCase" in {
      mustPass(
        LintRule.CamelCase(Document(
          Seq(),
          Seq(Struct(
            SimpleID("SomeType"),
            "SomeType",
            Seq(Field(1,
              SimpleID("camelCaseFieldName"),
              "camelCaseFieldName",
              TString)),
            None))))
      )
    }

    "fail CamelCase" in {
      val errors = LintRule.CamelCase(Document(
        Seq(),
        Seq(Struct(
          SimpleID("SomeType"),
          "SomeType",
          Seq(Field(1,
            SimpleID("non_camel_case"),
            "non_camel_case",
            TString)),
          None)))).toSeq
      errors.length must be(1)
      assert(errors.head.msg contains "lowerCamelCase")
    }

    "fail TransitivePersistence" in {
      val errors = LintRule.TransitivePersistence(
        Document(
          Seq(),
          Seq(
            struct(
              "SomeType",
              Map(
                "foo" -> TString,
                "bar" -> StructType(struct("SomeOtherType", Map.empty, persisted = false))
              ),
              persisted = true
            )
          )
        )).toSeq
      errors.length must be(1)
      val error = errors.head.msg
      assert(error.contains("persisted"))
      assert(error.contains("SomeType"))
      assert(error.contains("SomeOtherType"))
    }

    "fail TransitivePersistence for enums" in {
      val errors = LintRule.TransitivePersistence(
        Document(
          Seq(),
          Seq(
            struct(
              "SomeType",
              Map(
                "foo" -> TString,
                "bar" -> EnumType(enum("SomeEnumType", Seq.empty, persisted = false))
              ),
              persisted = true
            )
          )
        )).toSeq
      errors.length must be(1)
      val error = errors.head.msg
      assert(error.contains("persisted"))
      assert(error.contains("SomeType"))
      assert(error.contains("SomeEnumType"))
    }

    "pass TransitivePersistence" in {
      mustPass(LintRule.TransitivePersistence(
        Document(
          Seq(),
          Seq(
            struct(
              "SomeType",
              Map(
                "foo" -> TString,
                "bar" -> StructType(struct("SomeOtherType", Map.empty, persisted = true))
              ),
              persisted = true
            )
          )
        )))
    }

    "pass TransitivePersistence for enums" in {
      mustPass(LintRule.TransitivePersistence(
        Document(
          Seq(),
          Seq(
            struct(
              "SomeType",
              Map(
                "foo" -> TString,
                "bar" -> EnumType(enum("SomeOtherType", Seq.empty, persisted = true))
              ),
              persisted = true
            )
          )
        )))
    }

    "fail DocumentedPersisted" in {
      val errors = LintRule.DocumentedPersisted(
        Document(
          Seq(),
          Seq(
            struct(
              "SomeType",
              Map(
                "foo" -> TString
              ),
              persisted = true
            )
          )
        )).toSeq
      errors.length must be(2)
      assert(errors.forall(_.level == LintLevel.Warning))
      val structError = errors.head.msg
      assert(structError.contains("SomeType"))
      val fieldError = errors(1).msg
      assert(fieldError.contains("foo"))
      assert(fieldError.contains("SomeType"))
    }

    "pass DocumentedPersisted" in {
      mustPass(LintRule.DocumentedPersisted(
        Document(
          Seq(),
          Seq(
            Struct(
              SimpleID("SomeType"),
              "SomeType",
              Seq(Field(1,
                SimpleID("foo"),
                "foo",
                TString,
                docstring = Some("blah blah"))),
              docstring = Some("documented struct is documented"),
              Map("persisted" -> "true"))
          )
        )))
    }

    "pass RequiredFieldDefault" in {
      mustPass(
        LintRule.RequiredFieldDefault(Document(
          Seq(),
          Seq(Struct(
            SimpleID("SomeType"),
            "SomeType",
            Seq(
              Field(
                1,
                SimpleID("f1"),
                "f1",
                TString,
                default = Some(StringLiteral("v1")),
                requiredness = Requiredness.Optional),
              Field(
                2,
                SimpleID("f2"),
                "f2",
                TString,
                default = None,
                requiredness = Requiredness.Required)),
            None))))
      )
    }

    "fail RequiredFieldDefault" in {
      val errors = LintRule.RequiredFieldDefault(Document(
        Seq(),
        Seq(Struct(
          SimpleID("SomeType"),
          "SomeType",
          Seq(
            Field(
              1,
              SimpleID("f1"),
              "f1",
              TString,
              default = Some(StringLiteral("v1")),
              requiredness = Requiredness.Required)),
          None))))
      errors.length must be(1)
      assert(errors.head.msg contains "Required field")
    }

    "pass Keywords" in {
      mustPass(
        LintRule.Keywords(Document(
          Seq(),
          Seq(Struct(
            SimpleID("SomeType"),
            "SomeType",
            Seq(
              Field(
                1,
                SimpleID("klass"),
                "klass",
                TString,
                default = Some(StringLiteral("v1")),
                requiredness = Requiredness.Optional),
              Field(
                2,
                SimpleID("notAKeyWord"),
                "notAKeyWord",
                TString,
                default = None,
                requiredness = Requiredness.Required)),
            None))))
      )
    }

    "fail Keywords" in {
      val errors = LintRule.Keywords(Document(
        Seq(),
        Seq(Struct(
          SimpleID("SomeType"),
          "SomeType",
          Seq(
            Field(
              1,
              SimpleID("val"),
              "val",
              TString,
              default = Some(StringLiteral("v1")),
              requiredness = Requiredness.Optional)),
          None)))).toSeq
      errors.length must be(1)
      assert(errors.head.msg contains "Avoid using keywords")
    }

    "pass non negative index" in {
      mustPass(
        LintRule.FieldIndexGreaterThanZeroRule(Document(
          Seq(),
          Seq(Struct(
            SimpleID("SomeType"),
            "SomeType",
            Seq(
              Field(
                1,
                SimpleID("val"),
                "val",
                TString,
                default = Some(StringLiteral("v1")),
                requiredness = Requiredness.Optional)
            ),
            None))))
      )
    }

    "fail non negative index" in {
      val errors = LintRule.FieldIndexGreaterThanZeroRule(Document(
        Seq(),
        Seq(Struct(
          SimpleID("SomeType"),
          "SomeType",
          Seq(
            Field(
              -1,
              SimpleID("val"),
              "val",
              TString,
              default = Some(StringLiteral("v1")),
              requiredness = Requiredness.Optional)
          ),
          None)))).toSeq
      errors.length must be(1)
      assert(errors.head.msg contains "Field id should be supplied")
    }
  }

  private[this] def struct(name: String, fields: Map[String, FieldType], persisted: Boolean = false) =
    Struct(
      SimpleID(name),
      name,
      fields.zipWithIndex.map {
        case ((fieldName, fieldType), i) => Field(i, SimpleID(fieldName), fieldName, fieldType)
      }.toSeq,
      None,
      if (persisted) Map("persisted" -> "true") else Map.empty
    )

  private[this] def enum(name: String, values: Seq[EnumField], persisted: Boolean = false) =
    Enum(
      SimpleID(name),
      values,
      None,
      if (persisted) Map("persisted" -> "true") else Map.empty
    )
}
