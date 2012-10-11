package com.twitter.scrooge

class ParseException(reason: String, cause: Throwable) extends Exception(reason, cause) {
  def this(reason: String) = this(reason, null)
}

class OnewayNotSupportedException(name: String)
  extends ParseException("oneway modifier not supported by Scrooge in function " + name)

class NegativeFieldIdException(name: String)
  extends ParseException("Negative user-provided id in field " + name)

class DuplicateFieldIdException(name: String)
  extends ParseException("Duplicate user-provided id in field " + name)

class RepeatingEnumValueException(name: String, value: Int)
  extends ParseException("Repeating enum value in " + name + ": " + value)

/**
 * ScroogeInternalException indicates a Scrooge bug
 */
class ScroogeInternalException(msg: String) extends Exception