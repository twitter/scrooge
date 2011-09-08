// ----- {{name}}

object {{name}} {
  trait Iface {{syncExtends}}{
{{syncFunctions}}
  }

  trait FutureIface {{asyncExtends}}{
{{asyncFunctions}}
  }

{{functionStructs}}
{{finagleClient}}
{{finagleService}}
{{ostrichServer}}
}
