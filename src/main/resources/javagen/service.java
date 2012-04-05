public class {{name}} {
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

{{functionStructs}}
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
