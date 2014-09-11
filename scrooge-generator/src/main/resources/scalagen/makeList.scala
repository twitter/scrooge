{{#mutable}}
mutable$Buffer({{#commify}}{{/commify}})
{{/mutable}}
{{^mutable}}
Seq()
{{/mutable}}
(if (mutable) "mutable$Buffer(" else "Seq(") +
  list.elems.map(constantValue(_)).mkString(", ") + ")"
