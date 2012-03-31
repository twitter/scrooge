// ----- {{name}}

public class {{name}} {
  public interface Iface {{syncExtends}}{
{{#syncFunctions}}
    {{function}};
{{/syncFunctions}}
  }

  interface FutureIface {{asyncExtends}}{
{{#asyncFunctions}}
    {{function}};
{{/asyncFunctions}}
  }

{{functionStructs}}
{{finagleClient}}
{{finagleService}}
{{ostrichServer}}
}
