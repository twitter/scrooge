package com.twitter.scrooge
package scalagen

import com.twitter.util.Eval
import org.apache.thrift.protocol._
import org.specs.Specification
import org.specs.matcher.Matcher
import org.specs.mock.JMocker

trait EvalHelper { self: JMocker =>
  case class matchEqualsTField(a: TField) extends Matcher[TField]() {
    def apply(v: => TField) = (
      v.equals(a),
      "%s equals %s".format(v, a),
      "%s does not equal %s".format(v, a)
    )
  }

  case class matchEqualsTList(a: TList) extends Matcher[TList]() {
    def apply(v: => TList) = (v.elemType == a.elemType && v.size == a.size, "%s equals %s".format(v, a), "%s does not equal %s".format(v, a))
  }

  case class matchEqualsTSet(a: TSet) extends Matcher[TSet]() {
    def apply(v: => TSet) = (v.elemType == a.elemType && v.size == a.size, "%s equals %s".format(v, a), "%s does not equal %s".format(v, a))
  }

  case class matchEqualsTMap(a: TMap) extends Matcher[TMap]() {
    def apply(v: => TMap) = (v.keyType == a.keyType && v.valueType == a.valueType && v.size == a.size, "%s equals %s".format(v, a), "%s does not equal %s".format(v, a))
  }

  def equal(a: TField) = will(matchEqualsTField(a))
  def equal(a: TList) = will(matchEqualsTList(a))
  def equal(a: TSet) = will(matchEqualsTSet(a))
  def equal(a: TMap) = will(matchEqualsTMap(a))

  val eval = new Eval

  def invoke(code: String): Any = eval.inPlace[Any](code)

  def compile(code: String) {
    eval.compile(code)
  }
}
