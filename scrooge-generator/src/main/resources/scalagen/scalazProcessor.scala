package {{package}}

import org.apache.thrift.protocol.TProtocol
import com.twitter.scrooge.ThriftFunction
import com.twitter.scrooge.ThriftProcessor
import scalaz.concurrent.Task

{{docstring}}
@javax.annotation.Generated(value = Array("com.twitter.scrooge.Compiler"), date = "{{date}}")
class {{ServiceName}}$Processor(iface: {{ServiceName}}[Task]) extends ThriftProcessor[{{ServiceName}}[Task]](iface) {

  protected val processMap = (
{{#syncFunctions}}
      "{{funcName}}" -> Fn${{funcName}} ::
{{/syncFunctions}}
      Nil
  ).toMap

{{#syncFunctions}}
  object Fn${{funcName}} extends ThriftFunction[{{ServiceName}}[Task], {{ServiceName}}.{{funcName}}$args]("{{funcName}}") {

    def decode(in: TProtocol) = {{ServiceName}}.{{funcName}}$args.decode(in)

    def getResult(iface: {{ServiceName}}[Task], args: {{ServiceName}}.{{funcName}}$args) =
{{#hasThrows}}
    try {
{{/hasThrows}}
      {{ServiceName}}.{{funcName}}$result(success = iface.{{funcName}}(args.request))
{{#hasThrows}}
    } catch {
{{#throws}}
      case e: {{typeName}} => {{ServiceName}}.{{funcName}}$result(e = Task(e))
{{/throws}}
    }    
{{/hasThrows}}
  }

{{/syncFunctions}}
}
