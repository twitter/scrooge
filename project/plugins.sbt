resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
   url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

resolvers += "maven" at "http://repo1.maven.org/maven2/"

resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.3.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.8.1")
addSbtPlugin("me.lessis" % "bintray-sbt" % "0.1.1")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.2.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.2.16")
