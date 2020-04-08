package com.twitter.scrooge.java_generator

import com.twitter.scrooge.ast.{Enum, EnumField, Identifier}
import com.twitter.scrooge.backend.{ServiceOption, WithJavaPassThrough}
import java.util.{Map => JMap, Set => JSet}
import scala.collection.JavaConverters._

class EnumConstant(val name: String, val value: Int, val last: Boolean)
class EnumValueAnnotation(val name: String, val annotations: JSet[JMap.Entry[String, String]])

class EnumController(
  e: Enum,
  serviceOptions: Set[ServiceOption],
  generator: ApacheJavaGenerator,
  ns: Option[Identifier])
    extends TypeController(e, generator, ns) {

  val is_passthrough_enum: Boolean = serviceOptions.contains(WithJavaPassThrough)

  val constants: Seq[EnumConstant] = e.values.zipWithIndex map {
    case (v, i) =>
      new EnumConstant(v.sid.name, v.value, i == e.values.size - 1)
  }

  val struct_annotations: JSet[JMap.Entry[String, String]] = e.annotations.asJava.entrySet()

  val has_struct_annotations: Boolean = !struct_annotations.isEmpty

  val value_annotations: JSet[JMap.Entry[EnumField, EnumValueAnnotation]] = e.values
    .filter(_.annotations.nonEmpty).map { value =>
      value -> new EnumValueAnnotation(
        name = value.sid.name,
        annotations = value.annotations.asJava.entrySet()
      )
    }.toMap.asJava.entrySet()

  val has_value_annotations: Boolean = !value_annotations.isEmpty

}
