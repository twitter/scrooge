package com.twitter.scrooge;

import com.twitter.finagle.Service;
import com.twitter.util.Future;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.TMemoryBuffer;
import org.apache.thrift.transport.TMemoryInputTransport;
import org.apache.thrift.transport.TTransport;
import scala.Function2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

abstract public class FinagleThriftService extends Service<byte[], byte[]> {
  Map<String, Function2<TProtocol, Integer, Future<byte[]>>> functionMap =
    new HashMap<String, Function2<TProtocol, Integer, Future<byte[]>>>();
  TProtocolFactory protocolFactory;

  protected FinagleThriftService(TProtocolFactory protocolFactory) {
    this.protocolFactory = protocolFactory;
  }

  protected void addFunction(String name, Function2<TProtocol, Integer, Future<byte[]>> fn) {
    functionMap.put(name, fn);
  }

  protected Function2<TProtocol, Integer, Future<byte[]>> getFunction(String name) {
    return functionMap.get(name);
  }

  protected Future<byte[]> exception(String name, int seqid, int code, String message) {
    try {
      TApplicationException x = new TApplicationException(code, message);
      TMemoryBuffer memoryBuffer = new TMemoryBuffer(512);
      TProtocol oprot = protocolFactory.getProtocol(memoryBuffer);

      oprot.writeMessageBegin(new TMessage(name, TMessageType.EXCEPTION, seqid));
      x.write(oprot);
      oprot.writeMessageEnd();
      oprot.getTransport().flush();
      return Future.value(Arrays.copyOfRange(memoryBuffer.getArray(), 0, memoryBuffer.length()));
    } catch (Exception e) {
      return Future.exception(e);
    }
  }

  protected Future<byte[]> reply(String name, int seqid, ThriftStruct result) {
    try {
      TMemoryBuffer memoryBuffer = new TMemoryBuffer(512);
      TProtocol oprot = protocolFactory.getProtocol(memoryBuffer);

      oprot.writeMessageBegin(new TMessage(name, TMessageType.REPLY, seqid));
      result.write(oprot);
      oprot.writeMessageEnd();

      return Future.value(Arrays.copyOfRange(memoryBuffer.getArray(), 0, memoryBuffer.length()));
    } catch (Exception e) {
      return Future.exception(e);
    }
  }

  public final Future<byte[]> apply(byte[] request) {
    TTransport inputTransport = new TMemoryInputTransport(request);
    TProtocol iprot = protocolFactory.getProtocol(inputTransport);

    try {
      TMessage msg = iprot.readMessageBegin();
      Function2<TProtocol, Integer, Future<byte[]>> f = functionMap.get(msg.name);
      if (f != null) {
        return f.apply(iprot, msg.seqid);
      } else {
        TProtocolUtil.skip(iprot, TType.STRUCT);
        return exception(msg.name, msg.seqid, TApplicationException.UNKNOWN_METHOD, "Invalid method name: '" + msg.name + "'");
      }
    } catch (Exception e) {
      return Future.exception(e);
    }
  }
}
