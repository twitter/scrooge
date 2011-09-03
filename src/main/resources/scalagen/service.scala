// ----- {{name}}

object {{name}} {
  trait Iface {{extends}}{
{{syncFunctions}}
  }

  trait FutureIface {{extends}}{
{{asyncFunctions}}
  }

{{functionStructs}}
{{finagleClient}}
{{finagleService}}
{{ostrichServer}}
}
