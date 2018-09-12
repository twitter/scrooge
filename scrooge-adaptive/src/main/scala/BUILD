scala_library(
    sources = rglobs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "scrooge-adaptive",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/asm",
        "3rdparty/jvm/asm:asm-commons",
        "3rdparty/jvm/asm:asm-util",
        "3rdparty/jvm/org/apache/thrift:libthrift",
        "scrooge/scrooge-core",
        "scrooge/scrooge-serializer",
        "util/util-codec/src/main/scala",
        "util/util-logging/src/main/scala",
    ],
    exports = [
        "3rdparty/jvm/asm",
    ],
)
