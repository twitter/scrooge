package {{package}};

import com.twitter.scrooge.Option;
import com.twitter.scrooge.ThriftStruct;
import com.twitter.scrooge.ThriftStructCodec;
import com.twitter.scrooge.ThriftStructCodec3;
import com.twitter.scrooge.Utilities;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import org.apache.thrift.protocol.*;
import org.apache.thrift.TApplicationException;
{{#withFinagle}}
import com.twitter.finagle.Service;
import com.twitter.finagle.SourcedException;
import com.twitter.finagle.stats.Counter;
import com.twitter.finagle.stats.NullStatsReceiver;
import com.twitter.finagle.stats.StatsReceiver;
import com.twitter.finagle.thrift.ThriftClientRequest;
import com.twitter.util.Function2;
import com.twitter.util.Function;
import com.twitter.util.Future;
import com.twitter.util.FutureEventListener;
import java.util.Arrays;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TMemoryBuffer;
import org.apache.thrift.transport.TMemoryInputTransport;
import org.apache.thrift.transport.TTransport;
{{/withFinagle}}

{{docstring}}
@javax.annotation.Generated(value = "com.twitter.scrooge.Compiler")
public class {{ServiceName}} {
  public static interface Iface {{#syncParent}}extends {{syncParent}} {{/syncParent}}{
{{#syncFunctions}}
    {{>function}};
{{/syncFunctions}}
  }

{{#withFinagle}}
  public static interface FutureIface {{#futureIfaceParent}}extends {{futureIfaceParent}} {{/futureIfaceParent}}{
{{#asyncFunctions}}
    {{>function}};
{{/asyncFunctions}}
  }

  public static class FinagledClient extends {{ServiceName}}$FinagleClient {
      public FinagledClient (
        com.twitter.finagle.Service<com.twitter.finagle.thrift.ThriftClientRequest, byte[]> service,
        TProtocolFactory protocolFactory,
        String serviceName,
        com.twitter.finagle.stats.StatsReceiver stats
        ) {
          super(service, protocolFactory, serviceName, stats);
        }
  }

  public static class FinagledService extends {{ServiceName}}$FinagleService {
      public FinagledService (
        FutureIface iface,
        TProtocolFactory protocolFactory
        ) {
          super(iface, protocolFactory);
      }
  }

{{/withFinagle}}

{{#internalStructs}}
{{#internalArgsStruct}}
  {{>struct}}
{{/internalArgsStruct}}
{{#internalResultStruct}}
  {{>struct}}
{{/internalResultStruct}}
{{/internalStructs}}
{{#finagleClients}}
  {{>finagleClient}}
{{/finagleClients}}
{{#finagleServices}}
  {{>finagleService}}
{{/finagleServices}}
}
