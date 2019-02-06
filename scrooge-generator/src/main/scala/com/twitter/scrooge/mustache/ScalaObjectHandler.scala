package com.twitter.scrooge.mustache

import com.github.mustachejava.Iteration
import com.github.mustachejava.reflect.ReflectionObjectHandler
import java.io.Writer
import java.lang.reflect.{Field, Method}
import runtime.BoxedUnit
import scala.collection.JavaConverters._
import scala.reflect.ClassTag

/**
 * This class is borrowed from Mustache.java in scala-extensions. Its package
 * has been renamed from com.twitter.mustache to com.twitter.scrooge.mustache,
 * so it won't collide if a user also has included the scala-extensions jar in
 * their classpath.
 */
private[scrooge] class ScalaObjectHandler extends ReflectionObjectHandler {

  // Allow any method or field
  override def checkMethod(member: Method): Unit = {}

  override def checkField(member: Field): Unit = {}

  override def coerce(value: AnyRef) = {
    value match {
      case m: scala.collection.Map[_, _] =>
        // TODO: when we stop supporting scala 2.11, use JavaConverters.mapAsJavaMap
        m.asJava
      case u: BoxedUnit => null
      case Some(some: AnyRef) => coerce(some)
      case None => null
      case _ => value
    }
  }

  override def iterate(
    iteration: Iteration,
    writer: Writer,
    value: AnyRef,
    scopes: Array[AnyRef]
  ) = {
    value match {
      case TraversableAnyRef(t) => {
        var newWriter = writer
        t foreach { next =>
          newWriter = iteration.next(newWriter, coerce(next), scopes)
        }
        newWriter
      }
      case n: Number =>
        if (n.intValue() == 0) writer else iteration.next(writer, coerce(value), scopes)
      case _ => super.iterate(iteration, writer, value, scopes)
    }
  }

  override def falsey(
    iteration: Iteration,
    writer: Writer,
    value: AnyRef,
    scopes: Array[AnyRef]
  ) = {
    value match {
      case TraversableAnyRef(t) => {
        if (t.isEmpty) {
          iteration.next(writer, value, scopes)
        } else {
          writer
        }
      }
      case n: Number =>
        if (n.intValue() == 0) iteration.next(writer, coerce(value), scopes) else writer
      case _ => super.falsey(iteration, writer, value, scopes)
    }
  }

  val TraversableAnyRef = new Def[Traversable[AnyRef]]
  class Def[C: ClassTag] {
    def unapply[X: ClassTag](x: X): Option[C] = {
      x match {
        case c: C => Some(c)
        case _ => None
      }
    }
  }
}
