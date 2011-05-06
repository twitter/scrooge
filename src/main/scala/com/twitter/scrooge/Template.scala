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
  val eval = new Eval()

  def getName(klazz: Class[_]): String = {
    if (klazz.isArray()) {
      "Array[" + getName(klazz.getComponentType) + "]"
    } else {
      klazz.getName + klazz.getTypeParameters.map {_.getName}.mkString(",")
    }
  }

  def execute[A: Manifest](code: String, obj: T, scope: A): String = {
    val wrappedCode =
"""{
  (__param: """ + getName(manifest[T].erasure) + ", scope: " + getName(manifest[A].erasure) + """) => {
    import __param._
    import scope._
    val __rv = {
      """ + code + """
    }
    try {
      __rv.asInstanceOf[String]
    } catch {
      case e: Throwable =>
        throw new Exception("Unable to convert to string: (" + __rv + ") when eval'ing (""" + code.quoteC + """)", e)
    }
  }
}"""
    eval[(T, A) => String](wrappedCode)(obj, scope)
  }

  def execute(code: String, obj: T): String = execute[AnyRef](code, obj, null)

  def apply[A: Manifest](obj: T, scope: A): String = {
    text.regexSub("\\{\\{(.*?)\\}\\}".r) { m =>
      execute(m.group(1), obj, scope)
    }
  }

  def apply(obj: T): String = apply[AnyRef](obj, null)
}
