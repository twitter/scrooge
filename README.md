# scrooge

Scrooge is a thrift code generator for scala.

It's meant to be a drop-in replacement for the apache thrift code generator.

Since scala is API-compatible with java, you can use the apache thrift code
generator to generate java files and use them from within scala, but the
overhead of converting to/from java containers can sometimes be annoying.
This is an attempt to bypass the problem by generating scala code directly.
It also uses scala syntax so the generated code is *much* smaller.

The generated code still depends on libthrift.

There is a fairly comprehensive set of unit tests, which actually generate
code, compile it, and execute it to verify expectations.

There are two sub-projects:

- scrooge-generator: the actual code generator
- scrooge-runtime: some base traits used by the generated code


## Building

To build scrooge, use sbt:

    $ sbt package-dist


## Features

- Generates native scala thrift codecs, using case classes and functions.

- Generated code is templated using a mustache variant, making it easy to
  edit.

- Finagle client/server adaptors, and Ostrich wrappers, can be optionally
  generated at the same time.


## Running Scrooge

A starter script is built into `dist/scrooge/scripts`. You can run that or
write your own.

To get command line help:

    $ scrooge --help

To generate source with content written to the current directory:

    $ scrooge <thrift-file1> [<thrift-file2> ...]

To generate source with content written to a specified directory, using
extra include paths, rebuilding only those files that have changed:

    $ scrooge
      -d <target-dir>
      -i <include-path>
      -s
      <thrift-file1> [<thrift-file2> ...]


## Runtime dependency

There are a couple of classes needed by the generated code. These have been
moved out of scrooge into a separate jar to keep dependencies small:

    val scrooge_runtime = "com.twitter" % "scrooge-runtime" % "1.0.3"


## SBT Plugin

There's a plugin for SBT (0.11):

[https://github.com/twitter/sbt-scrooge](https://github.com/twitter/sbt-scrooge)

To use it, add a line like this to your `plugins.sbt` file:

    addSbtPlugin("com.twitter" %% "sbt11-scrooge" % "1.0.0")

(or whatever the current version is). Full details are in the sbt-scrooge
`README`.


## Finagle integration

If you pass the `--finagle` option to scrooge, it will generate a finagle
client and server wrapper class for each thrift service.

The service wrapper takes a thrift protocol factory (which specifies which
wire protocol to use) and an implementation of the future-based interface:

    class FinagledService(
      iface: FutureIface,
      val protocolFactory: TProtocolFactory
    ) extends FinagleThriftService

Here's an example of creating a finagle service using this class, assuming
your thrift service in named `AwesomeService`:

    val address = new InetSocketAddress(listenAddress, port)
    var builder = ServerBuilder()
      .codec(ThriftServerFramedCodec())
      .name("awesome_service")
      .bindTo(address)
    val protocol = new TBinaryProtocol.Factory()
    // calling build() in finagle is equivalent to calling start().
    builder.build(new AwesomeService.FinagledService(myService, protocol))

The client wrapper has a more complex interface, but is easy to use:

    class FinagledClient(
      val service: FinagleService[ThriftClientRequest, Array[Byte]],
      val protocolFactory: TProtocolFactory = new TBinaryProtocol.Factory,
      override val serviceName: Option[String] = None,
      stats: StatsReceiver = NullStatsReceiver
    ) extends FinagleThriftClient with FutureIface

It implements the future-based interface of the thrift service:

    val service = ClientBuilder()
      .hosts(new InetSocketAddress(host, port))
      .codec(ThriftClientFramedCodec())
      .build()
    val client = new AwesomeService.FinagledClient(service)

In both the server and client cases, you probably want to pass more
configuration parameters to finagle, so check the finagle documentation for
tweaks once you get things to compile.


## Implementation Semantics

Thrift is severely underspecified with respect to the handling of
required/optional/unspecified-requiredness and default values in various cases
such as serialization, deserialization, and new instance creation, and
different implementations do different things (see
http://lionet.livejournal.com/66899.html for a good analysis).

Scrooge attempts to be as rigourous as possible in this regard with
consistently applied and hopefully easy to understand rules.

1. If neither "required" nor "optional" is declared for a field, it then has
   the default requiredness of "optional-in/required-out", or "optInReqOut"
   for short.
2. It is invalid for a required field to be null and an exception will be
   thrown if you attempt to serialize a struct with a null required field.
3. It is invalid for a required field to be missing during deserialization,
   and an exception will be thrown in this case.
4. Optional fields can be set or unset, and the set-state is meaningful state
   of the struct that should be preserved by serialization/deserialization.
   Un-set fields are not present in the serialized representation of the
   struct.
5. Declared default values will be assigned to any non-required fields that
   are missing during deserialization. If no default is declared for a field,
   a default value appropriate for the type will be used (see below).
6. \#4 and \#5 imply that optional-with-default-value is not a tenable
   combination, and will be treated as if "optional" was not specified
   (optInReqOut-with-default-value).

### Default values by type

- bool = false
- byte/i16/i32/i64/double = 0
- string/struct/enum = null
- list = Seq()
- set = Set()
- map = Map()

The following "matrix" defines all the scenarios where a value may not be
present and how that case is handled:

#### required, no declared default value:
- missing in deserialization:
    - throws TProtocolException
- null in serialization:
    - throws TProtocolException
- immutable instance instantiation:
    - must be explicitly provided

#### required, with declared default value:
- missing in deserialization:
    - throws TProtocolException
- null in serialization:
    - throws TProtocolException
- immutable instance instantiation:
    - declared default value

#### optInReqOut, no declared default value:
- missing in deserialization:
    - default value for type
- null in serialization:
    - throws TProtocolException
- immutable instance instantiation:
    - must be explicitly provided

#### optInReqOut, with declared default value:
- missing in deserialization:
    - declared default value
- null in serialization:
    - throws TProtocolException
- immutable instance instantiation:
    - declared default value

#### optional, no declared default value:
- missing in deserialization:
    - None
- None in serialization:
    - omitted
- immutable instance instantiation:
    - None

#### optional, with declared default value:
- case not valid, treated as optInReqOut with declared default value


## License

Scrooge is licensed under the Apache 2 license, which you can find in the
included file `LICENSE`.


## Credits / Thanks

- Jorge Ortiz
- Robey Pointer
- Ian Ownbey
- Jeremy Cloud
