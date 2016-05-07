
scalaVersion := "2.11.8"

enablePlugins(BuildInfoPlugin)

libraryDependencies ++= Seq(
  "org.apache.thrift" % "libthrift" % "0.8.0",
  "com.twitter" %% "scrooge-core" % "4.6.0",
  "com.twitter" %% "finagle-thrift" % "6.34.0"
)
