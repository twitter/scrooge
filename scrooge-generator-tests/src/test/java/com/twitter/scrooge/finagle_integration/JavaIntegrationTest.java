package com.twitter.scrooge.finagle_integration;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import com.twitter.finagle.Addresses;
import com.twitter.finagle.JavaFailureFlags;
import com.twitter.finagle.ListeningServer;
import com.twitter.finagle.Names;
import com.twitter.finagle.ThriftMux;
import com.twitter.finagle.thrift.AbstractThriftService;
import com.twitter.scrooge.finagle_integration.thriftjava.BarService;
import com.twitter.scrooge.finagle_integration.thriftjava.InvalidQueryException;
import com.twitter.scrooge.finagle_integration.thriftjava.RegressionStruct;
import com.twitter.util.Await;
import com.twitter.util.Duration;
import com.twitter.util.Future;

import static org.junit.Assert.*;

public class JavaIntegrationTest {

  private RegressionStruct regressionReturnValue =
    new RegressionStruct(Collections.singletonList("Regression"));

  @Test
  public void test() throws Exception {
    BarService.ServiceIface serverImpl = new BarServiceImpl();
    ListeningServer server =
        ThriftMux.server().serveIface("localhost:*", serverImpl);

    BarService.ServiceIface client =
        ThriftMux.client().build(
            Names.bound(Addresses.newInetAddress((InetSocketAddress) server.boundAddress())),
            "a_client",
            BarService.ServiceIface.class);

    assertTrue(serverImpl instanceof AbstractThriftService);
    assertTrue(client instanceof AbstractThriftService);
    assertEquals(Await.result(client.echo("echo"), Duration.fromSeconds(5)), "echo");
    assertEquals(Await.result(client.duplicate("double"), Duration.fromSeconds(5)), "doubledouble");
    assertEquals(Await.result(client.getDuck(10L), Duration.fromSeconds(5)), "Scrooge");
    assertNull(Await.result(client.setDuck(20L, "McDuck"), Duration.fromSeconds(5)));
    assertEquals(Await.result(client.regression(Collections.singleton("regression")),
      Duration.fromSeconds(5)), regressionReturnValue);
    Await.result(server.close(), Duration.fromSeconds(5));
  }

  private class BarServiceImpl implements BarService.ServiceIface {

    @Override
    public Future<String> echo(String x) {
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

    @Override
    public Future<RegressionStruct> regression(Set<String> arg) {
      return Future.value(regressionReturnValue);
    }
  }

  @Test
  public void exceptions() {
    InvalidQueryException ex = new InvalidQueryException(5);
    assertEquals(JavaFailureFlags.EMPTY, ex.flags());

    InvalidQueryException rejected = ex.asRejected();
    assertEquals(JavaFailureFlags.REJECTED, rejected.flags());

    // verify flags is included in `equals` and `hashCode`
    assertNotEquals(ex, rejected);
    assertNotEquals(ex.hashCode(), rejected.hashCode());

    // verify flags are cleared by `clear`
    rejected.clear();
    assertEquals(JavaFailureFlags.EMPTY, rejected.flags());
  }

}
