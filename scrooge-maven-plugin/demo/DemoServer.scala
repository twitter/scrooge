import java.net.InetSocketAddress
import com.twitter.util.Future
import java.util.concurrent.atomic.AtomicInteger
import com.twitter.mydemo.renamed.{User, UserService}
import com.twitter.finagle.thrift.{ThriftServerFramedCodec, ThriftClientFramedCodec, ThriftClientRequest}
import com.twitter.finagle.builder.{ServerBuilder, ClientBuilder}
import org.apache.thrift.protocol.TBinaryProtocol


object DemoServer {
  private val userIdCounter: AtomicInteger = new AtomicInteger(0)

  // need to provide an implementation for the finagle service
  class MyUserImpl extends UserService.FutureIface {
    def createUser(name: String): Future[User] = {
      val id = userIdCounter.incrementAndGet()
      Future.value(User(id, name))
    }
  }

  def buildServer() = {
    val protocol = new TBinaryProtocol.Factory()
    val serverService = new UserService.FinagledService(new MyUserImpl, protocol)
    ServerBuilder()
      .codec(ThriftServerFramedCodec())
      .name("binary_service")
      .bindTo(new InetSocketAddress(0))
      .build(serverService)
  }
}
