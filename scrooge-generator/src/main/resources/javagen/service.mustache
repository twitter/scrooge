package {{package}};

import com.twitter.scrooge.ScroogeOption;
import com.twitter.finagle.SourcedException;
import com.twitter.scrooge.ThriftStruct;
import com.twitter.scrooge.ThriftStructCodec;
import com.twitter.scrooge.Utilities;
import com.twitter.util.Future;
import com.twitter.util.FutureEventListener;
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
{{#imports}}
import {{parentpackage}}.{{subpackage}}.*;
{{/imports}}
{{#finagleClient}}
import com.twitter.finagle.stats.NullStatsReceiver;
import com.twitter.finagle.stats.StatsReceiver;
import com.twitter.finagle.thrift.ThriftClientRequest;
import java.util.Arrays;
import org.apache.thrift.TException;
{{/finagleClient}}
{{#finagleService}}
import com.twitter.finagle.Service;
import com.twitter.finagle.stats.Counter;
import com.twitter.util.Function;
import com.twitter.util.Function2;
import org.apache.thrift.transport.TMemoryBuffer;
import org.apache.thrift.transport.TMemoryInputTransport;
import org.apache.thrift.transport.TTransport;
{{/finagleService}}
{{#ostrichServer}}
import com.twitter.finagle.builder.Server;
import com.twitter.finagle.builder.ServerBuilder;
import com.twitter.finagle.stats.StatsReceiver;
import com.twitter.finagle.thrift.ThriftServerFramedCodec;
import com.twitter.finagle.tracing.NullTracer;
import com.twitter.finagle.tracing.Tracer;
import com.twitter.logging.Logger;
{{/ostrichServer}}

{{docstring}}
public class {{ServiceName}} {
  public interface Iface {{syncExtends}}{
{{#syncFunctions}}
    {{>function}};
{{/syncFunctions}}
  }

  public interface FutureIface {{asyncExtends}}{
{{#asyncFunctions}}
    {{>function}};
{{/asyncFunctions}}
  }

{{#structs}}
  {{>struct}}
{{/structs}}
{{#finagleClients}}
  {{>finagleClient}}
{{/finagleClients}}
{{#finagleServices}}
  {{>finagleService}}
{{/finagleServices}}
{{#ostrichServers}}
  {{>ostrichServer}}
{{/ostrichServers}}
}
