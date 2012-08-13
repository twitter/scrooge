package com.twitter.scrooge

import org.specs.SpecificationWithJUnit

case class Car(make: String, model: String)

object Extra {
  val nameber = "WRX"
}

class TemplateSpec extends SpecificationWithJUnit {
  val car1 = Car("Subaru", "Impreza")

  "Template" should {
    "eval a code snippet" in {
      new Template[Car]("", Nil).execute("\"I saw a \" + make + \" \" + model", car1) mustEqual
        "I saw a Subaru Impreza"
    }

    "eval a template" in {
      val template = new Template[Car]("I saw a {{make}} {{model}} today!", Nil)
      template(car1) mustEqual "I saw a Subaru Impreza today!"
    }

    "eval with imports" in {
      val template = new Template[Car]("I saw a {{make}} {{model}} {{nameber}} today!",
        List("com.twitter.scrooge.Extra"))
      template(car1) mustEqual "I saw a Subaru Impreza WRX today!"
    }
  }
}


