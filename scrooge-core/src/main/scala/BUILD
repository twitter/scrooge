scala_library(
    sources = rglobs("*.scala"),
    provides = scala_artifact(
        org = "com.twitter",
        name = "scrooge-core",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/org/apache/thrift:libthrift",
        "util/util-core/src/main/scala",
    ],
    exports = [
        "3rdparty/jvm/org/apache/thrift:libthrift",
        "util/util-core/src/main/scala",
    ],
)
