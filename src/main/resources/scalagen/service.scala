object {{name}} {
  trait Iface {{extends}}{
  {{#syncFunctions}}
    {{> function}}
  {{/syncFunctions}}
  }

  trait FutureIface {{extends}}{
  {{#asyncFunctions}}
    {{> function}}
  {{/asyncFunctions}}
  }

{{#functionStructs}}
  {{> struct}}
{{/functionStructs}}

{{finagleClient}}
{{finagleService}}
{{ostrichServer}}
}
