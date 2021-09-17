java_thrift_library(
    name = "thrift-scala",
    sources = ["**/*.thrift"],
    compiler = "scrooge",
    include_paths = [
        "scrooge/scrooge-generator-tests/src/test/thrift/relative/dir2",
        "scrooge/scrooge-generator-tests/src/test/thrift/relative/dir3",
    ],
    language = "scala",
    tags = [
        "bazel-incompatible-glob",  # See http://go/bazel-incompatible-glob
    ],
    dependencies = [
        "3rdparty/jvm/org/apache/thrift:libthrift",
        "scrooge/scrooge-generator-tests/src/test/thrift/relative/dir2:include-scala",
        "scrooge/scrooge-generator-tests/src/test/thrift/relative/dir3:include-scala",
    ],
)

java_thrift_library(
    name = "thrift-bazel-workaround-android",
    sources = ["**/*.thrift"],
    compiler = "scrooge",
    default_java_namespace = "android_thrift_default_namespace",
    include_paths = [
        "scrooge/scrooge-generator-tests/src/test/thrift/relative/dir2",
        "scrooge/scrooge-generator-tests/src/test/thrift/relative/dir3",
    ],
    language = "android",
    tags = [
        "bazel-incompatible-glob",  # See http://go/bazel-incompatible-glob
    ],
    dependencies = [
        "3rdparty/jvm/org/apache/thrift:libthrift",
    ],
)
