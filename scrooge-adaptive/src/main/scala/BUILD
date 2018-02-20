scala_library(
    sources = rglobs("*.scala"),
    fatal_warnings = True,
    provides = scala_artifact(
        org = "com.twitter",
        name = "scrooge-adaptive",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/asm",
        "3rdparty/jvm/asm:asm-commons",
        "3rdparty/jvm/asm:asm-util",
        "3rdparty/jvm/org/apache/thrift:libthrift-0.5.0",
        "scrooge/scrooge-core",
        "scrooge/scrooge-serializer",
        "util/util-codec/src/main/scala",
        "util/util-logging/src/main/scala",
    ],
    exports = [
        "3rdparty/jvm/asm",
    ],
)
