
scalaVersion := "2.11.8"

enablePlugins(BuildInfoPlugin)

libraryDependencies ++= Seq(
  "org.apache.thrift" % "libthrift" % sys.props("libthrift.version"),
  "com.twitter" %% "scrooge-core" % sys.props("scrooge-core.version"),
  "com.twitter" %% "finagle-thrift" % sys.props("finagle.version")
)
