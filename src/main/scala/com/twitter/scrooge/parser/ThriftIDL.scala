package com.twitter.scrooge.parser

class ParseException(reason: String) extends java.lang.Exception(reason)

object ThriftIDL extends Parser {
  def parse(input: String) =
    phrase(document)(new lexical.Scanner(input)) match {
      case Success(result, _) => result
      case x @ Failure(msg, z) => throw new ParseException(x.toString)
      case x @ Error(msg, _) => throw new ParseException(x.toString)
    }
}
