scala_library(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "scrooge-adaptive",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/org/apache/thrift:libthrift",
        "3rdparty/jvm/org/ow2/asm",
        "3rdparty/jvm/org/ow2/asm:asm-commons",
        "3rdparty/jvm/org/ow2/asm:asm-util",
        "scrooge/scrooge-core",
        "scrooge/scrooge-serializer",
        "util/util-codec/src/main/scala",
        "util/util-logging/src/main/scala",
    ],
    exports = [
        "3rdparty/jvm/asm",
    ],
)
