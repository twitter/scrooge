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

For both clients and servers you have three API choices to pick from:

* ``MethodPerEndpoint``
* ``ServicePerEndpoint``
* ``ReqRepServicePerEndpoint``

``MethodPerEndpoint`` represents each Thrift service endpoint, or function, as
a method of the generated service class. The signature of each method will
closely match the corresponding function as defined in the Thrift IDL. One
difference though is that the return type of each method will be modified to
return a ``Future`` of the expected value, instead of the value directly. As an
example, the ``fetchBlob`` function of the ``BinaryService`` would be a method
which takes a ``Long`` named 'id' as an argument, and returns a ``Future``
of a Java NIO ``ByteBuffer``, with ``ByteBuffer`` being used to represent a
binary value.

``ServicePerEndpoint`` represents each Thrift service endpoint, or function, as
a Finagle ``Service``, instead of a method. This allows methods to be filtered,
either individually or collectively, as a ``Filter`` can be composed with a
``Service`` but not a method. At its base, a Finagle ``Service`` is defined as a
function from a ``RequestType`` to a ``Future`` of a ``ResponseType``.
Therefore, a call to a ``Service`` requires a single argument. To handle the
discrepancy with Thrift methods, where endpoints can require multiple arguments,
an ``Args`` class is generated, with each value being a named endpoint
parameter. For the return type of the ``Service``, a type alias is used named
``SuccessType``, which represents the return type of the Thrift service
endpoint. As an example, the ``fetchBlob`` function of the ``BinaryService``
would be a ``Service[FetchBlob.Args, FetchBlob.SuccessType]``. The
``FetchBlob.Args`` class would contain an 'id' value of type ``Long``. Whereas
``FetchBlob.SuccessType`` would be a type alias for Java NIO's ``ByteBuffer``.

``ReqRepServicePerEndpoint`` is similar to ``ServicePerEndpoint``, except that
each endpoint is represented by a ``Service`` from a |c.t.scrooge.Request|_ to
a |c.t.scrooge.Response|_. These envelope types allow for the passing of header
information between clients and servers, similar to the way HTTP request and
response headers work. Note that exchanging header information *only* works
when using the ``com.twitter.finagle.ThriftMux`` protocol.

Terminology
-----------

If you are still using the deprecated API constructs, we recommend switching to the non-deprecated
code as it attempts to add clarity to the API.

Below is a guide which may be helpful for translating from the deprecated API to the non-deprecated.

===================               ================================
Deprecated                        Replacement
===================               ================================
FutureIface                       MethodPerEndpoint
MethodIface                       MethodPerEndpoint.apply()
MethodIfaceBuilder                MethodPerEndpointBuilder
BaseServiceIface                  ServicePerEndpoint
ServiceIface                      ServicePerEndpoint.apply()
ServiceIfaceBuilder               ServicePerEndpointBuilder
--                                ReqRepServicePerEndpoint
--                                ReqRepServicePerEndpoint.apply()
--                                ReqRepServicePerEndpointBuilder
===================               ================================

Creating a Server
-----------------

`MethodPerEndpoint`
~~~~~~~~~~~~~~~~~~~

To create a server, you need to provide an implementation of the ``MethodPerEndpoint``
interface and use it with Finagle's ``com.twitter.finagle.Thrift.Server``
or ``com.twitter.finagle.ThriftMux.Server`` class.

::

    class ServerImpl extends BinaryService.MethodPerEndpoint {
      def fetchBlob(id: Long): Future[ByteBuffer] = {
        ??? // your implementation here
      }
    }
    val server = Thrift.server.serveIface("host:port", new ServerImpl)

`ServicePerEndpoint`
~~~~~~~~~~~~~~~~~~~~

Additionally, Scrooge generates a ``ServicePerEndpoint`` which is a trait
defining a ``Service`` for each Thrift method.

::

    trait ServicePerEndpoint {
        def fetchBlob: Service[BinaryService.FetchBlob.Args, BinaryService.FetchBlob.SuccessType])

Note that every method in the IDL becomes a ``Service`` for the corresponding
``Args`` and ``SuccessType`` structures. The wrappers are needed to wrap multiple
method arguments into one type. Instead of implementing the ``MethodPerEndpoint`` interface
directly, you can provide an instance of the ``ServicePerEndpoint`` and convert it to
the ``MethodPerEndpoint`` interface using the ``toThriftService`` function.

::

    val servicePerEndpoint: BinaryService.ServicePerEndpoint = new BinaryService.ServicePerEndpoint {
      def fetchBlob: Service[BinaryService.FetchBlob.Args, BinaryService.FetchBlob.SuccessType] = ???
    }

    val methodPerEndpoint: BinaryService.MethodPerEndpoint = servicePerEndpoint.toThriftService
    val service = Thrift.server.serveIface("host:port", methodPerEndpoint)

The advantage of this approach is that the ``Services`` can be decorated with
``Filters``.

::

    val servicePerEndpoint: BinaryService.ServicePerEndpoint = new BinaryService.ServicePerEndpoint {
      def fetchBlob: Service[BinaryService.FetchBlob.Args, BinaryService.FetchBlob.SuccessType] = ???
    }

    val loggingFilter: Filter[BinaryService.FetchBlob.Args, BinaryService.FetchBlob.SuccessType] = ???
    val filteredServicePerEndpoint: BinaryService.ServicePerEndpoint =
      servicePerEndpoint
        .withFetchBlob(
          fetchBlob = loggingFilter.andThen(servicePerEndpoint.fetchBlob)
        )

    val methodPerEndpoint: BinaryService.MethodPerEndpoint = filteredServicePerEndpoint.toThriftService
    val service = Thrift.server.serveIface("host:port", methodPerEndpoint)

`ReqRepServicePerEndpoint`
~~~~~~~~~~~~~~~~~~~~~~~~~~

.. note::

    Only the `ThriftMux` protocol supports passing header information.

Lastly, Scrooge generates a ``ReqRepServicePerEndpoint`` which is also a trait
defining a ``Service`` for each Thrift method.

::

    trait ReqRepServicePerEndpoint {
        def fetchBlob: Service[Request[BinaryService.FetchBlob.Args], Response[BinaryService.FetchBlob.SuccessType]])

Note that every method in the IDL becomes a ``Service`` over ``c.t.scrooge.Request[Args]`` and
``c.t.scrooge.Response[SuccessType]`` structures. These wrappers allow for the getting and setting
of header data that can be exchanged between a client and server. Again, instead of implementing the
``MethodPerEndpoint`` interface directly, you can also provide an instance of the
``ReqRepServicePerEndpoint`` and convert it to the ``MethodPerEndpoint`` interface using the
``toThriftService`` function.

::

    val reqRepServicePerEndpoint: BinaryService.ReqRepServicePerEndpoint = new BinaryService.ReqRepServicePerEndpoint {
      def fetchBlob: Service[Request[BinaryService.FetchBlob.Args], Response[BinaryService.FetchBlob.SuccessType]] = ???
    }

    val methodPerEndpoint: BinaryService.MethodPerEndpoint = reqRepServicePerEndpoint.toThriftService
    val service = ThriftMux.server.serveIface("host:port", methodPerEndpoint)

As true with the previous ``ServicePerEndpoint`` approach, this can also be decorated with ``Filters``.

::

    val reqRepServicePerEndpoint: BinaryService.ReqRepServicePerEndpoint = new BinaryService.ReqRepServicePerEndpoint {
      def fetchBlob: Service[Request[BinaryService.FetchBlob.Args], Response[BinaryService.FetchBlob.SuccessType]] = ???
    }

    val loggingFilter: Filter[Request[BinaryService.FetchBlob.Args], Response[BinaryService.FetchBlob.SuccessType]] = ???
    val filteredReqRepServicePerEndpoint: BinaryService.ReqRepServicePerEndpoint =
      reqRepServicePerEndpoint
        .withFetchBlob(
          fetchBlob = loggingFilter.andThen(reqRepServicePerEndpoint.fetchBlob)
        )

    val methodPerEndpoint: BinaryService.MethodPerEndpoint = filteredReqRepServicePerEndpoint.toThriftService
    val service = ThriftMux.server.serveIface("host:port", methodPerEndpoint)

.. important::

    It is expected that a ``MethodPerEndpoint`` is what is eventually served to create a `ListeningServer`, e.g.,

    `Protocol.server.serveIface(..., methodPerEndpoint)`

    Thus, even when implementing a ``ServicePerEndpoint`` or a ``ReqRepServicePerEndpoint``, the implementation
    should be converted to a ``MethodPerEndpoint`` (via `#toThriftService`) then served.

Creating a Client
-----------------

`MethodPerEndpoint`
~~~~~~~~~~~~~~~~~~~

Creating a client works similarly; you provide Finagle's ``Thrift.Client``
or ``ThriftMux.Client`` object the ``MethodPerEndpoint``.

::

    val methodPerEndpoint: BinaryService.MethodPerEndpoint =
      Thrift.client.build[BinaryService.MethodPerEndpoint]("host:port")

    val result: Future[ByteBuffer] = methodPerEndpoint.fetchBlob(12525L)

`ServicePerEndpoint`
~~~~~~~~~~~~~~~~~~~~

Alternatively, you can request a ``ServicePerEndpoint`` instead.

::

    val servicePerEndpoint: BinaryService.ServicePerEndpoint =
      Thrift.client.servicePerEndpoint[BinaryService.ServicePerEndpoint]("host:port")

    val result: Future[ByteBuffer] =
      servicePerEndpoint.fetchBlob(BinaryService.FetchBlob.Args(12525L))

As in the server case, this allows you to decorate your client with ``Filters``
and convert to the ``MethodPerEndpoint``.

::

    val timeoutFilter: Filter[BinaryService.FetchBlob.Args, BinaryService.FetchBlob.SuccessType] = ???
    val filteredServicePerEndpoint: BinaryService.ServicePerEndpoint =
      servicePerEndpoint
        .withFetchBlob(
          fetchBlob = timeoutFilter.andThen(servicePerEndpoint.fetchBlob)
        )

    val methodPerEndpoint: BinaryService.MethodPerEndpoint =
      Thrift.Client.methodPerEndpoint(filteredServicePerEndpoint)

`ReqRepServicePerEndpoint`
~~~~~~~~~~~~~~~~~~~~~~~~~~

.. note::

   Only the `ThriftMux` protocol supports passing header information.

Lastly, you can request a ``ReqRepServicePerEndpoint``.

::

   val reqRepServicePerEndpoint: BinaryService.ReqRepServicePerEndpoint =
      ThriftMux.client.servicePerEndpoint[BinaryService.ReqRepServicePerEndpoint]("host:port")

    val result: Future[ByteBuffer] =
      reqRepServicePerEndpoint.fetchBlob(
        Request(BinaryService.FetchBlob.Args(12525L))
          .setHeader("foo", "bar"))

As in the server case, this also allows you to decorate your client with ``Filters``
and convert to the ``MethodPerEndpoint``.

::

    val timeoutFilter: Filter[Request[BinaryService.FetchBlob.Args], Response[BinaryService.FetchBlob.SuccessType]] = ???
    val filteredReqRepServicePerEndpoint: BinaryService.ReqRepServicePerEndpoint =
      reqRepServicePerEndpoint
        .withFetchBlob(
          fetchBlob = timeoutFilter.andThen(reqRepServicePerEndpoint.fetchBlob)
        )

    val methodPerEndpoint: BinaryService.MethodPerEndpoint =
      ThriftMux.Client.methodPerEndpoint(filteredReqRepServicePerEndpoint)

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
