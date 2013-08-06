Ostrich Integration
===================

**NOTE: Ostrich is on the path to deprecation. Please use TwitterServer and the Finagle 6 APIs instead**

To use the deprecated ostrich integration

::

    object BinaryService {
      trait Iface { ... }
      trait FutureIface  { ... }
    }

To use the generated code Ostrich server:

::

    //First, you need to provide an implementation, as seen previously in the "--finagle" example
    import com.twitter.scrooge.OstrichThriftServer
    class MyImpl extends BinaryService.FutureIface { ... }
    val ostrichServer = new OstrichThriftServer with MyImpl {
      // server configuration
      val thriftPort = ..
      val serverName = ..
    }
    ostrichServer.start()

