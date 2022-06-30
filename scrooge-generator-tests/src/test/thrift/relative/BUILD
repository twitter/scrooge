java_thrift_library(
    name = "thrift-scala",
    sources = ["**/*.thrift"],
    compiler = "scrooge",
    language = "scala",
    tags = ["bazel-compatible"],
    dependencies = [
        "scrooge/scrooge-generator-tests/src/test/thrift/relative/dir2:include-scala",
        "scrooge/scrooge-generator-tests/src/test/thrift/relative/dir3:include-scala",
    ],
)

java_thrift_library(
    name = "thrift-bazel-workaround-android",
    sources = ["**/*.thrift"],
    compiler = "scrooge",
    default_java_namespace = "android_thrift_default_namespace",
    language = "scala",
    tags = ["bazel-compatible"],
    dependencies = [
        "scrooge/scrooge-generator-tests/src/test/thrift/relative/dir2:include-scala",
        "scrooge/scrooge-generator-tests/src/test/thrift/relative/dir3:include-scala",
    ],
)

resources(
    name = "resources",
    sources = [
        "*",
        "dir1/*.thrift",
        "dir1/dir4/*.thrift",
    ],
    tags = ["bazel-compatible"],
    dependencies = [
        "scrooge/scrooge-generator-tests/src/test/thrift/relative/dir2:resources",
        "scrooge/scrooge-generator-tests/src/test/thrift/relative/dir3:resources",
    ],
)
