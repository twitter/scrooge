resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
   url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.6.2")

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.1.1")

addSbtPlugin("net.virtual-void" % "sbt-cross-building" % "0.8.1")
