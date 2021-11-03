THRIFT_VALIDATION = ["com/twitter/scrooge/thrift_validation/*.scala"]

scala_library(
    sources = ["**/*.scala"] + exclude_globs(THRIFT_VALIDATION),
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "scrooge-core",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/javax/annotation:javax.annotation-api",
        "3rdparty/jvm/org/apache/thrift:libthrift",
        "scrooge/scrooge-core/src/main/scala/com/twitter/scrooge/thrift_validation",
        "util/util-core:scala",
        "util/util-validator/src/main/scala/com/twitter/util/validation",
    ],
    exports = [
        "3rdparty/jvm/org/apache/thrift:libthrift",
        "scrooge/scrooge-core/src/main/scala/com/twitter/scrooge/thrift_validation",
        "util/util-core:scala",
    ],
)
