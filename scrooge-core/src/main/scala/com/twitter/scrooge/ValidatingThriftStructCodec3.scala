package com.twitter.scrooge

import com.twitter.scrooge.validation.Issue

abstract class ValidatingThriftStructCodec3[T <: ThriftStruct] extends ThriftStructCodec3[T] {

  /**
   * Checks that the struct is a valid as a new instance. If there are any missing required or
   * construction required fields, return a non-empty Seq of Issues.
   */
  def validateNewInstance(item: T): Seq[Issue]

  /**
   * Method that should be called on every field of a struct to validate new instances of that
   * struct. This should only called by the generated implementations of validateNewInstance.
   */
  final protected def validateField[U <: ValidatingThriftStruct[U]](any: Any): Seq[Issue] = {
    any match {
      // U is unchecked since it is eliminated by erasure, but we know that validatingStruct extends
      // from ValidatingThriftStruct. The code below should be safe for any ValidatingThriftStruct
      case validatingStruct: ValidatingThriftStruct[_] =>
        val struct: U = validatingStruct.asInstanceOf[U]
        struct._codec.validateNewInstance(struct)
      case map: collection.Map[_, _] =>
        map.flatMap {
          case (key, value) =>
            Seq(
              validateField(key),
              validateField(value)
            ).flatten
        }.toList
      case iterable: Iterable[_] => iterable.toList.flatMap(validateField)
      case option: Option[_] => option.toList.flatMap(validateField)
      case _ => Nil
    }
  }
}
