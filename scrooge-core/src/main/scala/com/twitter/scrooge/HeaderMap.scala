package com.twitter.scrooge

import com.twitter.io.Buf
import java.nio.charset.{StandardCharsets => JChar}

private[scrooge] object HeaderMap {
  val keyGroupByFn: ((Buf, Buf)) => String = { case (key: Buf, _) =>
    Buf.decodeString(key, JChar.UTF_8).toLowerCase
  }

  def apply(): HeaderMap = new HeaderMap(Map.empty[String, Seq[Buf]])

  def apply(values: Map[String, Seq[Buf]]): HeaderMap = {
    new HeaderMap(values)
  }
}

/**
 * Immutable Map of Header Keys Strings to Seq of Header `Buf` Values.
 */
class HeaderMap private (headers: Map[String, Seq[Buf]]) {

  /**
   * Optionally returns the value associated with a key.
   *
   * @param  key the key value
   * @return an option value containing the value associated with `key`
   *         in this map, or `None` if none exists.
   */
  def get(key: String): Option[Seq[Buf]] = headers.get(key)

  /**
   * Retrieves the value which is associated with the given key. If there is no value
   * associated with the given key, throws a `NoSuchElementException`.
   *
   * @param  key the key
   * @return the value associated with the given key, throws `NoSuchElementException` if
   *         none exists.
   */
  def apply(key: String): Seq[Buf] = headers.apply(key)

  /**
   * Tests whether this [[HeaderMap]] contains a binding for a key.
   *
   * @param key the key
   * @return `true` if there is a binding for `key` in this map, `false` otherwise.
   */
  def contains(key: String): Boolean = headers.contains(key)

  /**
   * Tests whether the [[HeaderMap]] is empty.
   *
   *  @return `true` if the map does not contain any key/value binding, `false` otherwise.
   */
  def isEmpty: Boolean = headers.isEmpty

  /**
   * Converts this [[HeaderMap]] to a map. Duplicate keys will be overwritten
   * by later keys: since this is an unordered collection, which key is in the resulting map
   * is undefined.
   *
   * @return a map containing all elements of this [[HeaderMap]].
   */
  def toMap: Map[String, Seq[Buf]] = headers

  /**
   * Converts this [[HeaderMap]] to a sequence by flattening to a `Seq[(Buf, Buf)]`
   * and thus repeats keys as first tuple member.
   *
   * @return a sequence of (Buf, Buf) tuples containing all elements of this [[HeaderMap]].
   */
  def toBufSeq: Seq[(Buf, Buf)] = for {
    (key, values) <- toMap.toSeq
    value <- values
  } yield (Buf.Utf8(key), value)

  override def toString: String = {
    s"HeaderMap(${headers.map { case (key, values) =>
      s"$key -> ${values.map(Buf.decodeString(_, JChar.UTF_8)).mkString(" ")}"}.mkString(", ")
    })"
  }
}
