package com.twitter.scrooge.finagle_integration;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;

import scala.collection.JavaConversions;

import org.apache.thrift.TApplicationException;
import org.junit.Test;

import com.twitter.finagle.Addresses;
import com.twitter.finagle.Filter;
import com.twitter.finagle.ListeningServer;
import com.twitter.finagle.Name$;
import com.twitter.finagle.Service;
import com.twitter.finagle.ThriftMux;
import com.twitter.finagle.thrift.RichServerParam;
import com.twitter.scrooge.finagle_integration.thriftjava.BarService;
import com.twitter.util.Await;
import com.twitter.util.Duration;
import com.twitter.util.ExceptionalFunction;
import com.twitter.util.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JavaIntegrationFilterTest {

  @Test
  public void test() throws Exception {
    // andThen with Identity just to show a "complex" filter chain
    Filter.TypeAgnostic filters =
        Filter.typeAgnosticIdentity()
            .andThen(new HandleIllegalArgumentExceptionsFilter());

    ListeningServer server =
        ThriftMux.server().serve(
            new InetSocketAddress(InetAddress.getLoopbackAddress(), 0),
            new BarService.Service(new BarServiceImpl(), filters, new RichServerParam()));

    BarService.ServiceIface client =
        ThriftMux.client().build(
            Name$.MODULE$.bound(JavaConversions.asScalaBuffer(
                Collections.singletonList(
                    Addresses.newInetAddress((InetSocketAddress) server.boundAddress())))),
            "client",
            BarService.ServiceIface.class);

    assertEquals(Await.result(client.echo("echo"), Duration.fromSeconds(5)), "echo");
    assertEquals(Await.result(client.echo("illegal"), Duration.fromSeconds(5)), "Scrooge");
    try {
      Await.result(client.echo("null"));
    } catch (TApplicationException t) {
      assertTrue(t.getMessage().contains(NullPointerException.class.getName()));
      assertTrue(t.getMessage().contains("FORCED"));
    }
  }

  private class HandleIllegalArgumentExceptionsFilter extends Filter.TypeAgnostic {
    @Override
    public <T, U> Filter<T, U, T, U> toFilter() {
      return new Filter<T, U, T, U>() {
        @Override
        public Future<U> apply(T request, Service<T, U> service) {
          return service.apply(request).rescue(new ExceptionalFunction<Throwable, Future<U>>() {
            @Override
            @SuppressWarnings("unchecked")
            public Future<U> applyE(Throwable t) throws Throwable {
              if (t instanceof IllegalArgumentException) {
                return (Future<U>) Future.value("Scrooge");
              }
              throw t;
            }
          });
        }
      };
    }
  }

  private class BarServiceImpl implements BarService.ServiceIface {

    @Override
    public Future<String> echo(String x) {
      if ("illegal".equals(x)) {
        throw new IllegalArgumentException("FORCED");
      } else if ("null".equals(x)) {
        throw new NullPointerException("FORCED");
      }
      return Future.value(x);
    }

    @Override
    public Future<String> duplicate(String y) {
      return Future.value(y + y);
    }

    @Override
    public Future<String> getDuck(long key) {
      return Future.value("Scrooge");
    }

    @Override
    public Future<Void> setDuck(long key, String value) {
      return Future.Void();
    }
  }
}
