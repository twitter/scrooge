resources(
    sources = [
        "!BUILD*",
        "**/*",
    ],
    tags = [
        "bazel-incompatible-glob",  # See http://go/bazel-incompatible-glob
    ],
)

java_thrift_library(
    name = "constant-sets-thrift-scala",
    sources = ["constant_sets.thrift"],
    compiler = "scrooge",
    language = "scala",
    tags = ["bazel-compatible"],
)

java_thrift_library(
    name = "constant-sets-thrift-bazel-workaround-android",
    sources = ["constant_sets.thrift"],
    compiler = "scrooge",
    default_java_namespace = "android_thrift_default_namespace",
    language = "android",
    dependencies = [
        "3rdparty/jvm/org/apache/thrift:libthrift",
    ],
)
