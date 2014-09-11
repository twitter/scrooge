package com.twitter.scrooge.frontend

class ParseException(reason: String, cause: Throwable) extends Exception(reason, cause) {
  def this(reason: String) = this(reason, null)
}

// severe errors
class NegativeFieldIdException(name: String)
  extends ParseException("Negative user-provided id in field " + name)

class DuplicateFieldIdException(name: String)
  extends ParseException("Duplicate user-provided id in field " + name)

class RepeatingEnumValueException(name: String, value: Int)
  extends ParseException("Repeating enum value in " + name + ": " + value)

class UnionFieldInvalidNameException(union: String, field: String)
  extends ParseException("Field " + field + " in union " + union + " is prohibited")


// warnings (non-severe errors). If the strict mode is on, Scrooge will throw these exceptions;
// otherwise it merely prints warnings.
class ParseWarning(reason: String, cause: Throwable)
  extends ParseException(reason, cause)
{
  def this(reason: String) = this(reason, null)
}

class UnionFieldRequiredException(union: String, field: String)
  extends ParseWarning("Field " + field + " in union " + union + " cannot be required")

class UnionFieldOptionalException(union: String, field: String)
  extends ParseWarning("Field " + field + " in union " + union + " cannot be optional")

object UnionFieldRequirednessException {
  def apply(union: String, field: String, requiredness: String): ParseWarning = {
    requiredness.toLowerCase match {
      case "required" => new UnionFieldRequiredException(union, field)
      case "optional" => new UnionFieldOptionalException(union, field)
    }
  }
}

class InvalidThriftFilenameException(filename: String, regex: String)
  extends ParseWarning("Thrift filename " + filename + " is invalid, did not pass this check: " + regex)

class KeywordException(id: String)
  extends ParseWarning(s"Identifier '$id' is invalid: it is a thrift keyword.")

/**
 * ScroogeInternalException indicates a Scrooge bug
 */
class ScroogeInternalException(msg: String) extends Exception
