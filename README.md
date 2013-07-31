# Scrooge
[![Build Status](https://secure.travis-ci.org/twitter/scrooge.png)](http://travis-ci.org/twitter/scrooge)

Scrooge is a [thrift](http://thrift.apache.org/) code generator written in
Scala, which currently generates code for Scala and Java.

It's meant to be a replacement for the apache thrift code generator, and
generates conforming, compatible binary codecs by building on top of
libthrift.

Since Scala is API-compatible with Java, you can use the apache thrift code
generator to generate Java files and use them from within Scala, but the
generated code uses Java collections and mutable "bean" classes, causing some
annoying boilerplate conversions to be hand-written. This is an attempt to
bypass the problem by generating Scala code directly. It also uses Scala
syntax so the generated code is much more compact.

There is a fairly comprehensive set of unit tests, which actually generate
code, compile it, and execute it to verify expectations.

Quick-start
-----------

There are a couple of classes needed by the generated code. These have been
moved out of scrooge into a separate jar to keep dependencies small.
Maven users need to add the following to the pom.xml file:

    <dependency>
      <groupId>com.twitter</groupId>
      <artifactId>scrooge-runtime_2.9.2</artifactId>
      <version>3.3.2</version>
    </dependency>

SBT users need this:

    val scrooge_runtime = "com.twitter" %% "scrooge-runtime" % "3.3.2"

Full Documentation
------------------

<http://twitter.github.io/scrooge/>



## Building Scrooge

To build scrooge, use sbt:

    $ ./sbt +publish-local


## Maven Plugin
We ship a [scrooge-maven-plugin](https://github.com/twitter/scrooge/tree/master/scrooge-maven-plugin) with Scrooge,
as well as an [example maven project](https://github.com/twitter/scrooge/tree/master/demos/scrooge-maven-demo).
Please refer to the [example pom file] (https://github.com/twitter/scrooge/tree/master/demos/scrooge-maven-demo/pom.xml)


## Finagle integration

You can generate [finagle](https://github.com/twitter/finagle) binding code
by passing the `--finagle` option to scrooge. For each thrift service, Scrooge
will generate a wrapper class that builds Finagle services both on the server
and client sides.

Here's an example, assuming your thrift service is

    service BinaryService {
      binary fetchBlob(1: i64 id)
    }

Scrooge generates the following wrapper class:

    import com.twitter.finagle.Service
    import com.twitter.finagle.thrift.{ThriftClientRequest,
      ThriftServerFramedCodec, ThriftClientFramedCodec}
    object BinaryService {
      // vanilla interface
      trait Iface {
        def fetchBlob(id: Long): ByteBuffer
      }

      // future-based Finagle interface
      trait FutureIface {
        def fetchBlob(id: Long): Future[ByteBuffer]
      }

      /*
       The server side service wrapper takes a thrift protocol factory (to
       specify which wire protocol to use) and an implementation of
       FutureIface
      */
      class FinagledService(
        iface: FutureIface,
        val protocolFactory: TProtocolFactory
      ) extends Service[Array[Byte], Array[Byte]]

      /*
       The client wrapper implements FutureIface.
      */
      class FinagledClient(
        val service: Service[ThriftClientRequest, Array[Byte]],
        val protocolFactory: TProtocolFactory = new TBinaryProtocol.Factory,
        override val serviceName: Option[String] = None,
        stats: StatsReceiver = NullStatsReceiver
      ) extends FutureIface {
        /*
          The method call encodes method name along with arguments in
          ThriftClientRequest and sends to the server, then decodes server
          response to reconstruct the return value.
        */
        def fetchBlob(id: Long): Future[ByteBuffer]
      }
    }

To create a server, you need to provide an implementation of FutureIface,
and use it with FinagledService:

    // provide an implementation of the future-base service interface
    class MyImpl extends BinaryService.FutureIface {
      ...
    }
    val protocol = new TBinaryProtocol.Factory()
    val serverService = new BinaryService.FinagledService(new MyImpl, protocol)
    val address = new InetSocketAddress(listenAddress, port)
    var builder = ServerBuilder()
      .codec(ThriftServerFramedCodec())
      .name("binary_service")
      .bindTo(address)
      .build(serverService)

Creating a client is easy, you just need to build a finagle thrift client
service to pass to FinagledClient.

    val service = ClientBuilder()
      .hosts(new InetSocketAddress(host, port))
      .codec(ThriftClientFramedCodec())
      .build()
    val client = new BinaryService.FinagledClient(service)

In both the server and client cases, you probably want to pass more
configuration parameters to finagle, so check the finagle documentation for
tweaks once you get things to compile.

## Ostrich Integration
If you pass the "--ostrich" option, Scrooge will generate a convenience
wrapper ThriftServer. Following the BinaryService example:

    import com.twitter.ostrich.admin.Service
    object BinaryService {
      trait Iface { ... }
      trait FutureIface  { ... }
      trait ThriftServer extends Service with FutureIface {
        val thriftPort: Int
        val serverName: String

        //You can override serverBuilder to provide additional configuration.
        def serverBuilder = ...

        // Ostrich interface implementation is generated. It operates on the server built by serverBuilder.
        def start() { ... }
        def shutdown() { ... }
      }
    }

To use the generated code Ostrich server:

    //First, you need to provide an implementation, as seen previously in the "--finagle" example
    class MyImpl extends BinaryService.FutureIface { ... }
    val ostrichServer = new MyImpl with ThriftServer {
      // server configuration
      val thriftPort = ..
      val serverName = ..
    }
    ostrichServer.start()

## Dependencies of the generated code
The plain code generated by Scrooge depends on apache libthrift and scrooge-runtime.
You'll need to add the following dependencies to your project, if you use maven (other
build systems are similar)

        <dependency>
          <groupId>org.apache.thrift</groupId>
          <artifactId>libthrift</artifactId>
        </dependency>
        <dependency>
          <groupId>com.twitter</groupId>
          <artifactId>scrooge-runtime</artifactId>
       </dependency>

If you specify --finagle option, you need to have the following additional dependencies

       <dependency>
         <groupId>com.twitter</groupId>
         <artifactId>util-core</artifactId>
       </dependency>
       <dependency>
         <groupId>com.twitter</groupId>
         <artifactId>finagle-core</artifactId>
       </dependency>
       <dependency>
         <groupId>com.twitter</groupId>
         <artifactId>finagle-thrift</artifactId>
       </dependency>

If you specify --ostrich option, in addition to the above finagle dependencies, you
still need:

       <dependency>
         <groupId>com.twitter</groupId>
         <artifactId>finagle-ostrich4</artifactId>
       </dependency>

## Implementation Semantics

Thrift is severely underspecified with respect to the handling of
required/optional/unspecified-requiredness and default values in various cases
such as serialization, deserialization, and new instance creation, and
different implementations do different things (see
http://lionet.livejournal.com/66899.html for a good analysis).

Scrooge attempts to be as rigorous as possible in this regard with
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


## Owners

- Jeff Smick


## Credits / Thanks

- Chunyan Song
- Jorge Ortiz
- Robey Pointer
- Ian Ownbey
- Jeremy Cloud
- Nick Kallen
- Kevin Oliver
- Dana Contreras
