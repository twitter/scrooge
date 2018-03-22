scala_library(
    sources = rglobs("*.scala"),
    fatal_warnings = True,
    provides = scala_artifact(
        org = "com.twitter",
        name = "scrooge-serializer",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/org/apache/thrift:libthrift",
        "scrooge/scrooge-core",
        "util/util-app",
        "util/util-codec",
    ],
    exports = [
        "scrooge/scrooge-core",
    ],
)
