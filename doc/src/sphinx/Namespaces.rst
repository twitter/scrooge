Namespaces
========================

When generating with a target language of "scala" (the default), scrooge will
first look for and use a "scala" namespace declaration.  If no scala  namespace
is specified, scrooge will then fall back to any "java" namespace declaration,
and lastly to the catch-all "*" namespace.

Apache's thrift generator, prior to version 0.9, will actually abort with a
parse error if it encounters a "scala" namespace declaration (or for any
language but a few known languages that it supports).  This means that adding a
"scala"  namespace declaration to your thrift file, while optimal for scrooge
users, will render your thrift file unparseable by many versions of apache's
thrift generator.

To allow for backwards-compatibility with Apache's thrift generator, scrooge
supports a special comment-based namespace declaration that Apache's thrift
generator will ignore. To use this syntax, use the line comment character '#',
immediately followed by '@'.  Apache's  thrift generator will treat the
remaining text as a line comment, but scrooge will interprent any following
namespace declaration as if not commented-out.  For example:

::

  namespace java com.twitter.tweetservice.thriftjava
  #@namespace scala com.twitter.tweetservice.thriftscala


