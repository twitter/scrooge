package com.twitter.scrooge

import com.twitter.conversions.string._
import com.twitter.util.Eval

class Template[T: Manifest](text: String) {
  def execute(code: String, obj: T): String = {
    val wrappedCode =
      "{ (__param: " + manifest.erasure.getName + ") => {\n" +
      "import __param._\n" +
      code + "\n}}"
    Eval[T => String](wrappedCode)(obj)
  }

  def apply(obj: T): String = {
    text.regexSub("\\{\\{(.*?)\\}\\}".r) { m =>
      execute(m.group(1), obj)
    }
  }
}
