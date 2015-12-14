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

Scrooge generates the following wrapper classes:

::

    import com.twitter.finagle.Service
    import com.twitter.finagle.thrift.{ThriftClientRequest,
      ThriftServerFramedCodec, ThriftClientFramedCodec}
    import com.twitter.util.Future

    /*
     The server side service wrapper takes a thrift protocol factory (to
     specify which wire protocol to use) and an implementation of
     BinaryService[Future]
    */
    class BinaryService$FinagleService(
      iface: BinaryService[Future],
      val protocolFactory: TProtocolFactory
    ) extends Service[Array[Byte], Array[Byte]]

    /*
     The client wrapper implements BinaryService[Future].
    */
    class BinaryService$FinagleClient(
      val service: Service[ThriftClientRequest, Array[Byte]],
      val protocolFactory: TProtocolFactory = new TBinaryProtocol.Factory,
      override val serviceName: String = "",
      stats: StatsReceiver = NullStatsReceiver
    ) extends BinaryService[Future] {
      /*
        The method call encodes method name along with arguments in
        ThriftClientRequest and sends to the server, then decodes server
        response to reconstruct the return value.
      */
      def fetchBlob(id: Long): Future[ByteBuffer]
    }

Creating a Server
-----------------

To create a server, you need to provide an implementation of the service
interface and use it with Finagle's ``Thrift`` object.

::

    // provide an implementation of the future-base service interface
    class MyImpl extends BinaryService[Future] {
      ...
    }
    val service = Thrift.serve("host:port", new MyImpl)

Additionally, Scrooge generates a ``ServiceIface`` which is a case class
containing a ``Service`` for each thrift method.

::

    case class ServiceIface(
      fetchBlob: Service[FetchBlob.Args, FetchBlob.Result]
    )

Note that every method in the IDL becomes a ``Service`` for the corresponding
``Args`` and ``Result`` structures. The wrappers are needed to wrap multiple
method arguments into one type.  Instead of implementing the service interface
directly, you can provide an instance of the ``ServiceIface`` and convert it to
the service interface.

::

    val fetchBlobService: Service[FetchBlob.Args, FetchBlob.Result] = // ...
    val serviceImpl = BinaryService.ServiceIface(
      fetchBlob = fetchBlobService
    )
    val service = Thrift.serve("host:port", Thrift.newMethodIface(serviceImpl))

The advantage of this approach is that the ``Services`` can be decorated with
``Filters``.

::

    val serviceImpl = BinaryService.ServiceIface(
      fetchBlob = loggingFilter andThen fetchBlobService
    )

Creating a Client
-----------------

Creating a client is easy, you just provide Finagle's ``Thrift`` object the
iface.

::

    val client = Thrift.newIface[BinaryService[Future]]("host:port")

Alternatively, you can request a ``ServiceIface`` instead.

::

    val clientServiceIface =
      Thrift.newIface[BinaryService.ServiceIface]("host:port")

As in the server case, this allows you to decorate your client with ``Filters``.

::

    val filteredClient = clientServiceIface.copy(
      fetchBlob = timeoutFilter andThen clientServiceIface.fetchBlob
    )

In both the server and client cases, you probably want to pass more
configuration parameters to finagle, so check the finagle documentation for
tweaks once you get things to compile.

Converting Between Function and Service
---------------------------------------

As we saw above, a ``ServiceIface`` can be converted into the service interface.
This allows you to use the more ergonomic service interface while still being
able to apply ``Filters`` to your client.

::

    val clientMethodIface = Thrift.newMethodIface(filteredClient)
    val result = clientMethodIface.fetchBlob(1L) // respects the timeoutFilter

You can also use the ``functionToService`` and ``serviceToFunction`` methods on
``ThriftMethod`` to convert between function and Service implementations of a
thrift method.

::

    val serviceImpl = BinaryService.ServiceIface(
      fetchBlob = FetchBlob.functionToService { id: Long =>
        // ...
      }
    )

    val result = FetchBlob.serviceToFunction(serviceImpl.fetchBlob)(1L)
