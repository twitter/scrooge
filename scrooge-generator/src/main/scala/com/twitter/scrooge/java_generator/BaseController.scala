package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast._
import com.google.common.base.{Function => GFunction}

class BaseController(generator: ApacheJavaGenerator, ns: Option[Identifier]) {
  val gen_hash_code = generator.genHashcode
  def has_namespace = !ns.isEmpty
  def namespace = ns.get.fullName
  val trim_regex = """\s+\n""".r
  val consolidate_newline_regex = """\n+""".r

  def newHelper(f: String => String) = new GFunction[String, String]() {
    override def apply(input: String): String = f(input)
  }

  val consolidate_newlines = newHelper { input =>
    val consolidated = consolidate_newline_regex.replaceAllIn(trim_regex
      .replaceAllIn(input, "\n"), "\n")
    consolidated.replaceAll("<br/>", "").replaceAll("&nbsp;", " ")
  }

  val newlines_to_spaces = newHelper { input => input.replaceAll("\n", " ") }

  val trim = newHelper { input => input.replaceAll("\n", "").trim }

  val cap = newHelper { input => input.capitalize }

  val constant_name = newHelper { input =>
    val constantName = new StringBuilder
    var isFirst = true
    var wasPrevUpper = false
    input.foreach { c =>
      if (c.isUpper && !isFirst && !wasPrevUpper) {
        constantName.append("_")
      }
      constantName.append(c.toUpper)
      isFirst = false
      wasPrevUpper = c.isUpper
    }
    constantName.toString()
  }

  val isset_field_id = newHelper { fieldName => "__" + fieldName.toUpperCase + "_ISSET_ID" }

  def i_2 = newHelper { input =>
    indent(input, 2)
  }

  def i_4 = newHelper { input =>
    indent(input, 4)
  }

  def indent(
      input: String,
      indentation: Int,
      skipFirst: Boolean = true,
      addLast: Boolean = true): String = {
    if (indentation > 0) {
      val items = input.split("\n").toSeq
      val strings = items.zipWithIndex map { case (v, i) =>
        if (skipFirst && i == 0) v else " " * indentation + v
      }
      strings.mkString("\n") + (if (addLast) "\n" else "")
    } else {
      input
    }
  }
}
