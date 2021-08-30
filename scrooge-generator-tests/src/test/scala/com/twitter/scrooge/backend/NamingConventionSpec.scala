package com.twitter.scrooge.backend

import com.twitter.scrooge.testutil.Spec

class NamingConventionSpec extends Spec {
  "Scala Generator" should {

    "follow naming conventions" in {
      import thrift.`def`.default._
      Constants.`val` must be(10)
      Constants.`_try` must be(123)

      val naughty = Naughty("car", 100)
      naughty.`type` must be("car")
      naughty.`abstract` must be(100)

      Super.Trait.getValue must be(20)
      Super.get(99) must be(Some(Super.Native))
      Super.valueOf("trait") must be(Some(Super.Trait))
    }
  }
}
