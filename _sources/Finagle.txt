Finagle Integration
===================

You can generate `finagle <https://github.com/twitter/finagle>`_ binding code
by passing the `--finagle` option to scrooge. For each thrift service, Scrooge
will generate a wrapper class that builds Finagle services both on the server
and client sides.

Here's an example, assuming your thrift service is

::

    service BinaryService {
      binary fetchBlob(1: i64 id)
    }

Scrooge generates the following wrapper class:

::

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
and use it with Finagle's Thrift object.

::

    // provide an implementation of the future-base service interface
    class MyImpl extends BinaryService.FutureIface {
      ...
    }
    val service = Thrift.serve("host:port", new MyImpl)

Creating a client is easy, you just provide Finagle's Thrift object the
iface.

::

    val client = Thrift.newIface[BinaryService.FutureIface]("host:port")

In both the server and client cases, you probably want to pass more
configuration parameters to finagle, so check the finagle documentation for
tweaks once you get things to compile.
