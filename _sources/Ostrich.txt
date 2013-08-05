Ostrich Integration
===================

**NOTE This is deprecated**

If you pass the "--ostrich" option, Scrooge will generate a convenience
wrapper ThriftServer. Following the BinaryService example:

::

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

::

    //First, you need to provide an implementation, as seen previously in the "--finagle" example
    class MyImpl extends BinaryService.FutureIface { ... }
    val ostrichServer = new MyImpl with ThriftServer {
      // server configuration
      val thriftPort = ..
      val serverName = ..
    }
    ostrichServer.start()

