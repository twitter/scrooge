package com.twitter.scrooge.backend

import org.specs.SpecificationWithJUnit

class NamingConventionSpec extends SpecificationWithJUnit {
  "Scala Generator" should {

    "follow naming conventions" in {
      import thrift.`def`.default._
      Constants.`val` mustEqual 10
      Constants.`try` mustEqual 123

      val naughty = Naughty("car", 100)
      naughty.`type` mustEqual "car"
      naughty.`abstract` mustEqual 100

      Super.Trait.getValue mustEqual 20
      Super.get(99) must beSome(Super.Native)
      Super.valueOf("trait") must beSome(Super.Trait)
    }
  }

  "Java Generator" should {
    "follow naming convention" in {
      import thrift.java_def._default_._ // package name "default" got rewritten in Java
      Constants.`val` mustEqual 10
      Constants._try_ mustEqual 123

      val naughty = new Naughty("car", 100)
      naughty.getType() mustEqual "car"
      naughty.getAbstract() mustEqual 100

      Super.TRAIT.getValue() mustEqual 20
      Super.findByValue(99) mustEqual Super.NATIVE
    }
  }
}
