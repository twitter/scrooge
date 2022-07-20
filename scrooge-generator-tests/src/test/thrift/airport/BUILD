java_thrift_library(
    name = "thrift-scala",
    sources = ["*.thrift"],
    compiler = "scrooge",
    language = "scala",
    platform = "java8",
    tags = ["bazel-compatible"],
)

java_thrift_library(
    # Renamed longer to assist bazel migration
    name = "thrift-bazel-workaround-android",
    sources = ["*.thrift"],
    compiler = "scrooge",
    language = "android",
    platform = "java8",
    tags = ["bazel-compatible"],
)

# namespaced and non-Finagle
java_thrift_library(
    name = "thrift-namespace-scala",
    sources = ["*.thrift"],
    compiler = "scrooge",
    language = "scala",
    namespace_map = {
        "thrift.test": "vanilla.test",
        "thrift.test1": "vanilla.test1",
        "thrift.test2": "vanilla.test2",
    },
    platform = "java8",
    tags = ["bazel-compatible"],
)

java_thrift_library(
    # Renamed longer to assist bazel migration
    name = "thrift-namespace-bazel-workaround-android",
    sources = ["*.thrift"],
    compiler = "scrooge",
    language = "android",
    namespace_map = {
        "androidthrift.test": "vanilla_android.test",
        "androidthrift.test1": "vanilla_android.test1",
        "androidthrift.test2": "vanilla_android.test2",
    },
    platform = "java8",
    tags = ["bazel-compatible"],
)

resources(
    name = "resources",
    sources = ["*.thrift"],
    tags = ["bazel-compatible"],
)
