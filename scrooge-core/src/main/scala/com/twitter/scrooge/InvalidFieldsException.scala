package com.twitter.scrooge

/**
 * An exception used to communicate when the fields on a struct are set
 * incorrectly and the struct cannot be built because of it.
 */
final class InvalidFieldsException(message: String) extends Exception(message)
