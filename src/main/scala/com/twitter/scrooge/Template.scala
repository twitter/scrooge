package com.twitter.scrooge

import com.twitter.conversions.string._
import com.twitter.util.Eval

object Template {
  object Implicits {
    implicit def optionToString[T](wrapped: T => Option[String]): (T => String) = wrapped.andThen { _.getOrElse("") }
    implicit def voidToString(wrapped: Unit): String = ""
    implicit def noneToString(wrapped: None.type): String = ""
  }

  def apply[T: Manifest](text: String) = new Template[T](text)
}

class Template[T: Manifest](text: String) {
  def execute[A: Manifest](code: String, obj: T, scope: A): String = {
    val wrappedCode =
      "{ (__param: " + manifest[T].erasure.getName + ", scope: " + manifest[A].erasure.getName + ") => {\n" +
      "import __param._\n" +
      "import scope._\n" +
      code + "\n}.asInstanceOf[String] }"
    Eval[(T, A) => String](wrappedCode)(obj, scope)
  }

  def execute(code: String, obj: T): String = execute[AnyRef](code, obj, null)

  def apply[A: Manifest](obj: T, scope: A): String = {
    text.regexSub("\\{\\{(.*?)\\}\\}".r) { m =>
      execute(m.group(1), obj, scope)
    }
  }

  def apply(obj: T): String = apply[AnyRef](obj, null)
}
