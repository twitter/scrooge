package com.twitter.scrooge

import org.specs.Specification

object TemplateSpec extends Specification {
  case class Car(make: String, model: String)

  val car1 = Car("Subaru", "Impreza")

  "Template" should {
    "eval a code snippet" in {
      new Template[Car]("").execute("\"I saw a \" + make + \" \" + model", car1) mustEqual
        "I saw a Subaru Impreza"
    }

    "eval a template" in {
      val template = new Template[Car]("I saw a {{make}} {{model}} today!")
      template(car1) mustEqual "I saw a Subaru Impreza today!"
    }
  }
}
