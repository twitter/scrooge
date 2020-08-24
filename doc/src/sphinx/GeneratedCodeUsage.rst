Generated Scala Code Usage
==========================

This section explains how to work with the Scala code that Scrooge generates.


Unions
------

Scrooge generates Scala case classes that can be used to construct union instances. Consider the following union which may have a string value or a User struct value::

    struct User {
        1: i64 id
        2: string name
    }

    union Visitor {
      1: string guestName
      2: User user
    }

To create an instance of Visitor, use the case classes named for each field::

    Visitor.GuestName("Alice")
    Visitor.User(User(123L, "Bob"))

The case classes for union fields can be used to access values in a typesafe way via pattern matching::

    val v: Visitor = Visitor.GuestName("Erica")
    val displayName = v match {
      case Visitor.GuestName(name) => "guest " + name
      case Visitor.User(u) => u.name
    }

Because new fields can be added to unions, Scrooge provides UnknownUnionField which should be used for compatibility with newer versions::

    val displayName = v match {
      case Visitor.GuestName(name) => "guest " + name
      case Visitor.User(u) => u.name
      case Visitor.UnknownUnionField => "unknown visitor"
    }

Note that you must use the provided extractors when pattern matching against unions; you cannot use the constructors for structs themselves. The following will fail with a MatchError::

    userVisitor match {
      case User(id, name) => name
      case s: String => s
    }

Optional functionality
----------------------

For `struct`\s, you can opt into generating
`proxy classes <https://en.wikipedia.org/wiki/Proxy_pattern>`_. The generated code
delegates to another instance. These may be useful for tests and otherwise.

Given the IDL:

::

    struct User {
      1: i64 id
      2: string name
    }

Add the annotation.

::

    struct User {
      1: i64 id
      2: string name
    } (com.twitter.scrooge.scala.generateStructProxy = "true")

Which generates the following code on the companion `User` object,

::

    trait Proxy extends User {
      protected def _underlying_User: User
      override def id: Long = _underlying_User.id
      override def name: String = _underlying_User.name
      override def _passthroughFields: immutable$Map[Short, TFieldBlob] = _underlying_User._passthroughFields
    }

Then, for example, `Proxy` can then be used in the following manner,

::

    val alwaysFifty: User = new User.Proxy {
      protected def _underlying_User: User = aUserInstance
      override def id: Long = 50
    }
