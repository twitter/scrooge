package com.twitter.scrooge

import com.twitter.util.Memoize
import scala.reflect.runtime.universe

private[scrooge] object Companions {

  /**
   * Given a class, finds the first companion object of the class
   * or its base classes that implements the specified type.
   * @tparam T the type of a companion object to find
   * @param c the class for which the companion is sought
   * @return an optionally found companion object
   */
  def findCompanion[T](c: Class[_])(implicit tt: universe.TypeTag[T]): Option[T] = {
    val runtimeMirror = universe.runtimeMirror(c.getClassLoader)

    runtimeMirror
      .classSymbol(c)
      .baseClasses.iterator
      .map(_.companion)
      .collect { case c if c.isModule => c.asModule }
      .find(_.moduleClass.asType.toType <:< tt.tpe)
      .map(runtimeMirror.reflectModule(_).instance.asInstanceOf[T])
  }

  /**
   * Given a type, creates a memoized function on Class objects that finds
   * the first companion object of the class or its base classes that
   * implements the specified type.
   * @tparam T the type of a companion object to find
   * @return a function of Class-to-Any that for every passed-in class
   *         returns its first companion object conforming to the type.
   *         The function throws an `IllegalArgumentException` when the
   *         class has no companion of the requested type.
   */
  def createMemoizedCompanionFinder[T](implicit tt: universe.TypeTag[T]): Class[_] => T = {
    Memoize.classValue { c =>
      findCompanion(c)
        .getOrElse(
          throw new IllegalArgumentException(
            s"No companion ${tt.tpe.typeSymbol} found for ${c.getName} or its base classes")
        )
    }
  }
}
