scala_library(
    sources = ["**/*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    platform = "java8",
    provides = scala_artifact(
        org = "com.twitter",
        name = "scrooge-serializer",
        repo = artifactory,
    ),
    tags = ["bazel-compatible"],
    dependencies = [
        "3rdparty/jvm/org/apache/thrift:libthrift",
        "scrooge/scrooge-core",
        "util/util-app",
        "util/util-codec",
    ],
    exports = [
        "scrooge/scrooge-core",
        "util/util-codec",
    ],
)
