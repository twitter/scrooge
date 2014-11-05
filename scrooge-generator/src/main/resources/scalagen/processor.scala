package {{package}}

import org.apache.thrift.protocol.TProtocol
import com.twitter.scrooge.ThriftFunction
import com.twitter.scrooge.ThriftProcessor

{{docstring}}
@javax.annotation.Generated(value = Array("com.twitter.scrooge.Compiler"), date = "{{date}}")
class {{ServiceName}}$Processor(iface: {{ServiceName}}[Some]) extends ThriftProcessor[{{ServiceName}}[Some]](iface) {

  protected val processMap = (
{{#syncFunctions}}
      "{{funcName}}" -> Fn${{funcName}} ::
{{/syncFunctions}}
      Nil
  ).toMap

{{#syncFunctions}}
  object Fn${{funcName}} extends ThriftFunction[{{ServiceName}}[Some], {{ServiceName}}.{{funcName}}$args]("{{funcName}}") {

    def decode(in: TProtocol) = {{ServiceName}}.{{funcName}}$args.decode(in)

    def getResult(iface: {{ServiceName}}[Some], args: {{ServiceName}}.{{funcName}}$args) = {
{{#hasThrows}}
    try {
{{/hasThrows}}
      // Note that the typeclass is Some, not Option, so .get is safe
      val value = iface.{{funcName}}({{argNames}}).get
      {{ServiceName}}.{{funcName}}$result({{resultNamedArg}})
{{#hasThrows}}
    } catch {
{{#throws}}
      case e: {{typeName}} => {{ServiceName}}.{{funcName}}$result(e = Some(e))
{{/throws}}
    }    
{{/hasThrows}}
  }
  }

{{/syncFunctions}}
}
