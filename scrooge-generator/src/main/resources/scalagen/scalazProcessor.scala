package {{package}}

import org.apache.thrift.protocol.TProtocol
import com.twitter.scrooge.ScalazThriftFunction
import com.twitter.scrooge.AsyncThriftFunction
import com.twitter.scrooge.AsyncThriftProcessor
import com.twitter.scrooge.ThriftStruct
import scalaz.concurrent.Task

{{docstring}}
@javax.annotation.Generated(value = Array("com.twitter.scrooge.Compiler"), date = "{{date}}")
class {{ServiceName}}$ScalazProcessor(iface: {{ServiceName}}[Task]) extends AsyncThriftProcessor[{{ServiceName}}[Task]](iface) {

  protected val processMap = (
{{#syncFunctions}}
      "{{funcName}}" -> Fn${{funcName}} ::
{{/syncFunctions}}
      Nil
  ).toMap[String, AsyncThriftFunction[{{ServiceName}}[Task]]]

{{#syncFunctions}}
  object Fn${{funcName}} extends ScalazThriftFunction[{{ServiceName}}[Task], {{ServiceName}}.{{funcName}}$args]("{{funcName}}") {

    def decode(in: TProtocol) = {
      {{ServiceName}}.{{funcName}}$args.decode(in)
    }

    def getResult(iface: {{ServiceName}}[Task], args: {{ServiceName}}.{{funcName}}$args) = {
      iface.{{funcName}}({{argNames}}).map {
        value =>
          {{ServiceName}}.{{funcName}}$result({{resultNamedArg}})}
      }
{{#hasThrows}}
        .handle {
{{#throws}}
          case e: {{typeName}} => {{ServiceName}}.{{funcName}}$result({{fieldName}} = Some(e))
{{/throws}}
        }
{{/hasThrows}}
    }

{{/syncFunctions}}
}
