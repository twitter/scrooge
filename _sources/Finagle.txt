Finagle Integration
===================

You can generate `Finagle <https://github.com/twitter/finagle>`_ binding code
by passing the `--finagle` option to scrooge. For each Thrift service, Scrooge
will generate classes that build Finagle Services for both client and server.

The following examples are for Scala generated code and use the
following Thrift IDL:

::

    service BinaryService {
      binary fetchBlob(1: i64 id)
    }

For both clients and servers you have two API choices to pick from —
``ServiceIface`` and ``MethodIface``. The ``ServiceIface`` is a
collection of Finagle ``Services``. By their nature of being ``Services``,
these play well with Finagle's standard ``Filter`` composition.
The ``MethodIface`` is a collection of methods returning ``Futures``
and is the primary implementation of a ``FutureIface``.

Creating a Server
-----------------

To create a server, you need to provide an implementation of the ``FutureIface``
interface and use it with Finagle's ``com.twitter.finagle.Thrift.Server``
or ``com.twitter.finagle.ThriftMux.Server`` class.

::

    class MyImpl extends BinaryService.FutureIface {
      def fetchBlob(id: Long): Future[ByteBuffer] = {
        ??? // your implementation here
      }
    }
    val service = Thrift.server.serveIface("host:port", new MyImpl)

Additionally, Scrooge generates a ``ServiceIface`` which is a case class
containing a ``Service`` for each Thrift method.

::

    case class ServiceIface(
        fetchBlob: Service[BinaryService.FetchBlob.Args, BinaryService.FetchBlob.SuccessType])

Note that every method in the IDL becomes a ``Service`` for the corresponding
``Args`` and ``SuccessType`` structures. The wrappers are needed to wrap multiple
method arguments into one type. Instead of implementing the ``FutureIface`` interface
directly, you can provide an instance of the ``ServiceIface`` and convert it to
the ``FutureIface`` interface using the ``MethodBuilder`` constructor.

::

    val fetchBlobService: Service[BinaryService.FetchBlob.Args, BinaryService.FetchBlob.SuccessType] = ???
    val serviceIface: BinaryService.ServiceIface = BinaryService.ServiceIface(
      fetchBlob = fetchBlobService
    )
    val futureIface: BinaryService.FutureIface =
      new BinaryService.MethodIface(serviceIface)
    val service = Thrift.server.serve("host:port", futureIface)

The advantage of this approach is that the ``Services`` can be decorated with
``Filters``.

::

    val loggingFilter: Filter[BinaryService.FetchBlob.Args, BinaryService.FetchBlob.SuccessType] = ???
    val serviceImpl: BinaryService.ServiceIface = BinaryService.ServiceIface(
      fetchBlob = loggingFilter.andThen(fetchBlobService)
    )

Creating a Client
-----------------

Creating a client works similarly; you provide Finagle's ``Thrift.Client``
or ``ThriftMux.Client`` object the ``FutureIface``.

::

    val futureIface: BinaryService.FutureIface =
      Thrift.client.newIface[BinaryService.FutureIface]("host:port")

    val result: Future[ByteBuffer] = futureIface.fetchBlob(12525L)

Alternatively, you can request a ``ServiceIface`` instead.

::

    val serviceIface: BinaryService.ServiceIface =
      Thrift.client.newServiceIface[BinaryService.ServiceIface]("host:port")

    val result: Future[ByteBuffer] =
      serviceIface.fetchBlob(BinaryService.FetchBlob.Args(12525L))

As in the server case, this allows you to decorate your client with ``Filters``
and convert to the ``FutureIface``.

::

    val timeoutFilter: Filter[BinaryService.FetchBlob.Args, BinaryService.FetchBlob.SuccessType] = ???
    val filteredServiceIface: BinaryService.ServiceIface = serviceIface.copy(
      fetchBlob = timeoutFilter.andThen(serviceIface.fetchBlob)
    )
    val futureIface: BinaryService.FutureIface =
      new BinaryService.MethodIface(filteredServiceIface)

Configuration
-------------

In both the server and client cases, you probably want to pass more
configuration parameters to the Finagle client (e.g. ``Thrift.client.withXXX``),
please check the `Finagle documentation <https://twitter.github.io/finagle/guide/Configuration.html>`_
for tweaks once you get things to compile.

A common configuration is the Thrift ``TProtocolFactory`` which can
be set with ``com.twitter.finagle.Thrift.Server.withProtocolFactory``
and ``com.twitter.finagle.Thrift.Client.withProtocolFactory``
along with the same methods available for ThriftMux.

Converting Between Function and Service
---------------------------------------

As we saw above, a ``ServiceIface`` can be converted into the ``FutureIface`` interface.
This allows you to use a method-based interface while still being
able to apply ``Filters`` to your client.

::

    val timeoutFilter: Filter[BinaryService.FetchBlob.Args, BinaryService.FetchBlob.SuccessType] = ???
    val serviceIface: BinaryService.ServiceIface =
      Thrift.client.newServiceIface[BinaryService.ServiceIface]("host:port")
    val filteredServiceIface: BinaryService.ServiceIface = serviceIface.copy(
      fetchBlob = timeoutFilter.andThen(serviceIface.fetchBlob)
    )
    val methodIface: BinaryService.MethodIface =
      new BinaryService.MethodIface(filteredServiceIface)

    // respects the timeoutFilter
    val result: Future[ByteBuffer] = methodIface.fetchBlob(1L)

You can also use the ``functionToService`` and ``serviceToFunction`` methods on
``ThriftMethod`` to convert between function and ``Service`` implementations of a
Thrift method.

::

    val serviceIface: BinaryService.ServiceIface = BinaryService.ServiceIface(
      fetchBlob = BinaryService.FetchBlob.functionToService { id: Long =>
        ??? // implementation returning a Future[ByteBuffer]
      }
    )

    val fetchBlobFn: Function[Long, Future[ByteBuffer]] =
      BinaryService.FetchBlob.serviceToFunction(serviceIface.fetchBlob)
    val result: Future[ByteBuffer] = fetchBlobFn(1L)
