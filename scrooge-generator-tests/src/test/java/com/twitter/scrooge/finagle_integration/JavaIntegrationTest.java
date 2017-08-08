package com.twitter.scrooge.finagle_integration;

import java.net.InetSocketAddress;
import java.util.Collections;

import scala.collection.JavaConversions;

import org.junit.Test;

import com.twitter.finagle.Addresses;
import com.twitter.finagle.ListeningServer;
import com.twitter.finagle.Name$;
import com.twitter.finagle.ThriftMux;
import com.twitter.scrooge.finagle_integration.thriftjava.BarService;
import com.twitter.util.Await;
import com.twitter.util.Duration;
import com.twitter.util.Future;

import static org.junit.Assert.assertEquals;

public class JavaIntegrationTest {

  @Test
  public void test() throws Exception {
    ListeningServer server =
        ThriftMux.server().serveIface("localhost:*", new BarServiceImpl());

    BarService.ServiceIface client =
        ThriftMux.client().newIface(
            Name$.MODULE$.bound(JavaConversions.asScalaBuffer(
                Collections.singletonList(
                    Addresses.newInetAddress((InetSocketAddress) server.boundAddress())))),
            "a_client",
            BarService.ServiceIface.class);
    assertEquals(Await.result(client.echo("echo"), Duration.fromSeconds(5)), "echo");
    assertEquals(Await.result(client.duplicate("double"), Duration.fromSeconds(5)), "doubledouble");
    assertEquals(Await.result(client.getDuck(10L), Duration.fromSeconds(5)), "Scrooge");
    assertEquals(Await.result(client.setDuck(20L, "McDuck"), Duration.fromSeconds(5)), null);
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
  }

}
