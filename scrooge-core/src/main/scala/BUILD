scala_library(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "scrooge-core",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/javax/annotation:javax.annotation-api",
        "3rdparty/jvm/org/apache/thrift:libthrift",
        "util/util-core:scala",
    ],
    exports = [
        "3rdparty/jvm/org/apache/thrift:libthrift",
        "util/util-core:scala",
    ],
)
