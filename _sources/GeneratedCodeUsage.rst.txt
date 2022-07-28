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
      case Visitor.UnknownUnionField(_) => "unknown visitor"
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

Codecs and Metadata
-------------------
When you write code that works with a particular set of Scrooge-generated classes,
you know statically what fields do they have and what are their types. You also
have access to those classes' `ThriftStructCodec` simply as the companion object to
the struct class. However, if you write code that generally works with any Scrooge-generated
structs, Scrooge gives you APIs to obtain both their codec and the metadata describing their
fields. (Unions are structs too, so everything that applies to structs applies to unions as
well.)

How to get the codec for a struct class
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To get a `com.twitter.scrooge.ThriftStructCodec` for any struct class, use the
`ThriftStructCodec.forStructClass(Class)` method. Pass it the runtime class of a
Scrooge-generated struct and it will return its codec object:

.. code-block:: scala

    def structionize(s: ThriftStruct) = {
      val codec = ThriftStructCodec.forStructClass(s.getClass)
      ... use the codec
    }


Codecs have `encode` and `decode` methods as well as a `metaData` method that returns a
`com.twitter.scrooge.ThriftStructMetaData` object describing the structure in detail.

How to get metadata for a struct class
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can also obtain the metadata object directly using the
`ThriftStructMetaData.forStructClass(Class)` method. Metadata describes the struct, most notably
providing access to information about its fields (`fieldInfos` for structs and `unionFields`
for unions) as well as access to annotations with the `structAnnotations` method. The `fields`
method (separate from `fieldInfos`) provides access to `ThriftStructField` objects that most
notably expose a `getValue` method allowing you to retrieve the field value from a struct instance.
For example, to read an arbitrary named field's value you could use a method like this below:

.. code-block:: scala

    def readFieldValue[R](s: ThriftStruct, fieldName: String): Option[R] = {
      val metaData = ThriftStructMetaData.forStructClass(s.getClass)
      metaData.fields.collectFirst {
        case f if f.name == fieldName => f.getValue(s)
      }
    }

To get field annotations for a particular field you could use a method such as:

.. code-block:: scala

    def getFieldAnnotations(s: ThriftStruct, fieldName: String): Map[String, String] = {
      val metaData = ThriftStructMetaData.forStructClass(s.getClass)
      val annOpt = metaData.fieldInfos.collectFirst {
        case f if f.tfield.name == fieldName => f.fieldAnnotations
      }
      annOpt.getOrElse(Map.Empty)
    }

How to retrieve field information for a union arm
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Unions have one more special method. In Scrooge, union arms are represented as
subclasses of a top-level class representing the union itself. The
`com.twitter.scrooge.ThriftStructFieldInfo` for a particular arm can be accessed
using `ThriftUnion.fieldInfoForUnionClass(Class)` method. It is equivalent to calling
the `unionStructFieldInfo` on an instance of the union, but is helpful if you don't have
an instance of a union arm, just its runtime `Class` object. For example, if you have
an instance of a union arm, you can get all of its annotations using

.. code-block:: scala

    def unionArmAnnotations(u: ThriftUnion): Map[String, String] = {
      u.unionStructFieldInfo.map(_.fieldAnnotations).getOrElse(Map.Empty)
    }

However, if all you have is a runtime class for it, then you can use the class-specific
method:

.. code-block:: scala

    def unionArmAnnotations(uc: Class[_ <: ThriftUnion]): Map[String, String] = {
      ThriftUnion.fieldInfoForUnionClass(uc).map(_.fieldAnnotations).getOrElse(Map.Empty)
    }

(Note in above examples, both methods return an `Option[ThriftStructFieldInfo]` - if you pass
the instance/class representing the overall union, and not a specific arm, they return `None`.)
