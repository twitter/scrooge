
- handle optional/required [is there really anything to do there?]
x generate a struct for each args/return from a method
x handle "not present" fields in encoding (needed particularly for return-value structs)

a field can be:
  - in a struct:
    + required
    + optional
    + (default) optional in, required out
  - in an arg list:
    + required
    + (default) optional in, required out

"in": when read from a stream via decoder
"out": when created via case class constructor

1000-line Sqrt2Service.java:

Sqrt2Service {
  public interface Iface {
    public Rational sqrt2(int rounds) throws TException;
  }

  public interface AsyncIface {
    public void sqrt2(int rounds, AsyncMethodCallback<AsyncClient.sqrt2_call> resultHandler) throws TException;
  }

  public interface ServiceIface {
    public Future<Rational> sqrt2(int rounds);
  }

  public static class Client implements TServiceClient, Iface {
    public static class Factory implements TServiceClientFactory<Client> {
      public Client getClient(TProtocol prot);
      public Client getClient(TProtocol iprot, TProtocol oprot);
    }

    public TProtocol getInputProtocol()
    public TProtocol getOutputProtocol()
    public Rational sqrt2(int rounds) throws TException
    public void send_sqrt2(int rounds) throws TException
    public Rational recv_sqrt2() throws TException
  }

  public static class AsyncClient extends TAsyncClient implements AsyncIface {
    public static class Factory implements TAsyncClientFactory<AsyncClient> {
      public AsyncClient getAsyncClient(TNonblockingTransport transport);
    }
    public void sqrt2(int rounds, AsyncMethodCallback<sqrt2_call> resultHandler) throws TException {
    public static class sqrt2_call extends TAsyncMethodCall {
      public sqrt2_call(int rounds, AsyncMethodCallback<sqrt2_call> resultHandler, TAsyncClient client, TProtocolFactory protocolFactory, TNonblockingTransport transport) throws TException;
      public void write_args(TProtocol prot) throws TException;
      public Rational getResult() throws TException;
    }
  }

  public static class ServiceToClient implements ServiceIface {
    public Future<Rational> sqrt2(int rounds);
    public static class Processor implements TProcessor {
      public boolean process(TProtocol iprot, TProtocol oprot) throws TException;
    }
  }

  public static class Service extends com.twitter.finagle.Service<byte[], byte[]> {
    public Service(final ServiceIface iface, final TProtocolFactory protocolFactory);
    public Future<byte[]> apply(byte[] request);
  }

  public static class sqrt2_args implements TBase<sqrt2_args, sqrt2_args._Fields>, java.io.Serializable, Cloneable ...
  public static class sqrt2_result implements TBase<sqrt2_result, sqrt2_result._Fields>, java.io.Serializable, Cloneable ...
}
